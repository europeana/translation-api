package eu.europeana.api.translation.service.etranslation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.service.util.UtilityMethods;

public class ETranslationTranslationService extends AbstractTranslationService {
  
  private static final Logger LOGGER = LogManager.getLogger(ETranslationTranslationService.class);

  private String serviceId;
  private final String baseUrl;
  private final String domain;
  //this is the base url of the translation api (without the request handler (or the controller) endpoint)
  private final String callbackUrl;
  private final String credentialUsername;
  private final String credentialPwd;
  private final int maxWaitMillisec;
  private final RedisMessageListenerContainer redisMessageListenerContainer;
  public static final String baseUrlTests="base-url-for-testing";
  
  public ETranslationTranslationService(String baseUrl, String domain, String callbackUrl, int maxWaitMillisec, 
      String username, String password, RedisMessageListenerContainer redisMessageListenerContainer) throws TranslationException {
    if(!baseUrlTests.equals(baseUrl) && (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(domain) || StringUtils.isBlank(callbackUrl) ||
        maxWaitMillisec<=0 || StringUtils.isBlank(username) || StringUtils.isBlank(password))) {
      throw new TranslationException("Invalid eTranslation config parameters.");
    }
    this.baseUrl = baseUrl;
    this.domain = domain;
    this.callbackUrl=callbackUrl;
    this.maxWaitMillisec=maxWaitMillisec;
    this.credentialUsername=username;
    this.credentialPwd=password;
    this.redisMessageListenerContainer=redisMessageListenerContainer;
  }

  @Override
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    String sourceLang = translationObjs.get(0).getSourceLang();
    if(sourceLang==null) {
      throw new TranslationException("The source language cannot be null for the eTranslation service.");
    }

//    String eTranslJointStr = generateJointStringForTranslation(translationObjs, markupDelimit);
    String eTranslJointStr = generateJointHtmlForTranslation(translationObjs);
    
    /* create an eTransl request with an external reference and send it. The same external reference is received
     * in the eTransl callback. That reference is used for the name of the channel for the redis message subscriber 
     * listener created below, which will be notified from the redis publisher after the eTransl callback comes.
     * The publisher will publish to the same channel using the external reference from the eTransl callback.
     */
    //create external reference for eTransl service
    String eTranslExtRef = UtilityMethods.generateRedisKey(
        eTranslJointStr, translationObjs.get(0).getSourceLang(), translationObjs.get(0).getTargetLang(), true);

    //create and send the eTransl request
    //baseUrl is different for the integration tests, where the eTranslation service will not be called
    if(! baseUrlTests.equals(baseUrl)) {
      try {
        String body = createTranslationBody(eTranslJointStr,translationObjs.get(0).getSourceLang(),translationObjs.get(0).getTargetLang(),eTranslExtRef);
        String eTranslRespNumber = createHttpRequest(body);
        if(Integer.parseInt(eTranslRespNumber) < 0) {
          throw new TranslationException("Invalid eTranslation http request.");
        }
      } catch (JSONException | UnsupportedEncodingException e) {
        throw new TranslationException("Exception during the eTranslation http request body creation.", 0, e);
      } catch (IOException e) {
        throw new TranslationException("Exception during sending the eTranslation http request.", 0, e);
      }  
    }
      
    //create a redis message listener obj, and wait on that obj until it get notified from the redis publisher
    createRedisMessageListenerAndWaitForResults(translationObjs, eTranslExtRef);
      
  }
  
  private void createRedisMessageListenerAndWaitForResults(List<TranslationObj> translationObjs, String eTranslExtRef) throws TranslationException {
    RedisMessageListener redisMessageListener = new RedisMessageListener();
    MessageListenerAdapter redisMessageListenerAdapter = new MessageListenerAdapter(redisMessageListener);
    redisMessageListenerContainer.addMessageListener(redisMessageListenerAdapter, ChannelTopic.of(eTranslExtRef));
    synchronized (redisMessageListener) {
      /*
       * While loop as a good practice to ensure spurious wake-ups (https://www.baeldung.com/java-wait-notify).
       * In addition, time is measured to not wait again and again the same max time, in case of spurious wake-ups
       */
      long sleepTimeMillisec=0;
      while(redisMessageListener.getMessage()==null) {
        try {
          long goSleepTimeNanosec=System.nanoTime();
          if(sleepTimeMillisec < maxWaitMillisec) {
            redisMessageListener.wait(maxWaitMillisec - sleepTimeMillisec);
          }
          else {
            if(LOGGER.isDebugEnabled()) {
              LOGGER.debug("eTranslation response has not been received after waiting for: " + maxWaitMillisec + " milliseconds.");
            }
            break;
          }
          long wakeUpTimeNanosec = System.nanoTime();
          sleepTimeMillisec += (wakeUpTimeNanosec-goSleepTimeNanosec)/1000000.0;
        } catch (InterruptedException e) {
        }
      }
      
      String response=redisMessageListener.getMessage();
      //message received, populate the translations
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug("Received message from redis message listener is: " + response);
      }
      if(response!=null) {
        //first base64 decode
        String respBase64Decoded = new String(Base64.decodeBase64(response), StandardCharsets.UTF_8);
        Document jsoupDoc = Jsoup.parse(respBase64Decoded);
        Elements pTagTexts = jsoupDoc.select("p");
        if(pTagTexts.size()!=translationObjs.size()) {
          redisMessageListenerContainer.removeMessageListener(redisMessageListenerAdapter);
          throw new TranslationException("The eTranslation response and the input texts have different size.");
        }
        for(int i=0;i<pTagTexts.size();i++) {
          translationObjs.get(i).setTranslation(pTagTexts.get(i).ownText());
        }
      }
      /* unsubscibe this listener which automatically deletes the created pub/sub channel,
       * which also gets deleted if the app is stopped or anyhow broken.
       */
      redisMessageListenerContainer.removeMessageListener(redisMessageListenerAdapter);
    }
  }
  
  /**
   * Generate one eTransl html string to be sent for the translation, as a combination of all input texts. 
   * @throws TranslationException 
   */
  private String generateJointHtmlForTranslation(List<TranslationObj> translationObjs) throws TranslationException {
    StringBuilder translJointString=new StringBuilder();
    translJointString.append("<!DOCTYPE html>\n");
    translJointString.append("<htlm>\n");
    translJointString.append("<body>\n");
    for(int i=0;i<translationObjs.size();i++) {
      translJointString.append("<p>");
      translJointString.append(translationObjs.get(i).getText());
      translJointString.append("</p>\n");
    }
    translJointString.append("</body>\n");
    translJointString.append("</html>");

    return translJointString.toString();
    
  }
  /**
   * This method creates the translation request body with the text to translate. 
   * The response is sent back to the application over a specified callback URL 
   * (REST service).
   * 
   * @param text
   * @param sourceLang
   * @param targetLang
   * @param externalReference
   * @return
   * @throws JSONException
   * @throws UnsupportedEncodingException 
   */
  private String createTranslationBody(String text, String sourceLang, String targetLang, String externalReference) 
      throws JSONException {
    String base64EncodedText=Base64.encodeBase64String(text.getBytes(StandardCharsets.UTF_8));
    JSONObject jsonBody = new JSONObject().put("priority", 0)
//        .put("requesterCallback", callbackUrl)
//        .put("errorCallback", callbackUrl)
        .put("externalReference", externalReference)
        .put("callerInformation", new JSONObject().put("application", credentialUsername).put("username", credentialUsername))
        .put("sourceLanguage", sourceLang.toUpperCase(Locale.ENGLISH))
        .put("targetLanguages", new JSONArray().put(0, targetLang.toUpperCase(Locale.ENGLISH)))
        .put("domain", domain)
        .put("destinations",
//            new JSONObject().put("emailDestinations", new JSONArray().put(0, "e-mail")))
            new JSONObject().put("httpDestinations", new JSONArray().put(0, callbackUrl)))
//        .put("textToTranslate", text);
        .put("documentToTranslateBase64",
            new JSONObject().put("content", base64EncodedText).put("format", "html")
            );
    return jsonBody.toString();
  }

  private String createHttpRequest(String content) throws IOException {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(credentialUsername, credentialPwd));
    CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build();
    HttpPost request = new HttpPost(baseUrl);
    StringEntity params = new StringEntity(content, "UTF-8");
    request.addHeader("content-type", "application/json");
    request.setEntity(params);
    return EntityUtils.toString(httpClient.execute(request).getEntity(), "UTF-8");
  }
  
  @Override
  public String getServiceId() {
    return serviceId;
  }

  @Override
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  @Override
  public boolean isSupported(String srcLang, String trgLang) {
    return true;
  }

  @Override
  public void close() {
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

}
