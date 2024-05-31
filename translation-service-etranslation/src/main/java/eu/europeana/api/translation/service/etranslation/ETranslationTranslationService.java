package eu.europeana.api.translation.service.etranslation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.service.util.TranslationUtils;

public class ETranslationTranslationService extends AbstractTranslationService {
  
  private static final Logger LOGGER = LogManager.getLogger(ETranslationTranslationService.class);
  public static final String baseUrlTests="base-url-for-testing";
  public static final String markupDelimiter="\n[notranslate]deenPVsaOg[/notranslate]\n";//base64 encoded string (as in generateRedisKey()) with new lines
  public static final String markupDelimiterWithoutNewline=Pattern.quote("[notranslate]deenPVsaOg[/notranslate]");
  public static final String eTranslationErrorCallbackIndicator="eTranslationErrorCallback";
  public static final String eTranslationCallbackRelativeUrl="/etranslation/callback";
  public static final String eTranslationErrorCallbackRelativeUrl="/etranslation/error-callback";
  
  private String serviceId;
  private final String baseUrl;
  private final String domain;
  private final String translationApiBaseUrl;
  private final String credentialUsername;
  private final String credentialPwd;
  private final int maxWaitMillisec;
  private final RedisMessageListenerContainer redisMessageListenerContainer;
  
  public ETranslationTranslationService(String etranslationServiceBaseUrl, String domain, String translationApiBaseUrl, int maxWaitMillisec, 
      String username, String password, RedisMessageListenerContainer redisMessageListenerContainer) throws TranslationException {
    if(!baseUrlTests.equals(etranslationServiceBaseUrl)) {
      validateETranslConfigParams(etranslationServiceBaseUrl, domain, translationApiBaseUrl, maxWaitMillisec, username, password);
    }
    this.baseUrl = etranslationServiceBaseUrl;
    this.translationApiBaseUrl = translationApiBaseUrl;
    this.domain = domain;
    this.maxWaitMillisec=maxWaitMillisec;
    this.credentialUsername=username;
    this.credentialPwd=password;
    this.redisMessageListenerContainer=redisMessageListenerContainer;
  }

  private String getTranslationErrorCallbackUrl() {
    return this.translationApiBaseUrl + eTranslationErrorCallbackRelativeUrl;
  }

  private String getTranslatioCallbackUrl() {
    return this.translationApiBaseUrl + eTranslationCallbackRelativeUrl;
  }
  
  private void validateETranslConfigParams(String etranslationServiceBaseUrl, String domain, String translationApiBaseUrl,
      int maxWaitMillisec, String username, String password) throws TranslationException {
    List<String> missingParams= new ArrayList<>(6);
    if(StringUtils.isBlank(etranslationServiceBaseUrl)) {
      missingParams.add("baseUrl");
    }
    if(StringUtils.isBlank(domain)) {
      missingParams.add("domain");
    }
    if(StringUtils.isBlank(translationApiBaseUrl)) {
      missingParams.add("translationApiBaseUrl");
    }
    if(maxWaitMillisec<=0) {
      missingParams.add("maxWaitMillisec (must be >0)");
    }
    if(StringUtils.isBlank(username)) {
      missingParams.add("username");
    }
    if(StringUtils.isBlank(password)) {
      missingParams.add("password");
    }
    
    if(! missingParams.isEmpty()) {
      throw new TranslationException("Invalid eTranslation config parameters: " + missingParams.toString());
    }
  }

  @Override
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    String sourceLang = translationObjs.get(0).getSourceLang();
    if(sourceLang==null) {
      throw new TranslationException("The source language cannot be null for the eTranslation service.");
    }

    String eTranslJointStr = generateJointStringForTranslation(translationObjs);
//    String eTranslJointStr = generateJointHtmlForTranslation(translationObjs); //used as document translation
    
    /* create an eTransl request with an external reference and send it. The same external reference is received
     * in the eTransl callback. That reference is used for the name of the channel for the redis message subscriber 
     * listener created below, which will be notified from the redis publisher after the eTransl callback comes.
     * The publisher will publish to the same channel using the external reference from the eTransl callback.
     */
    //create external reference for eTransl service
    String eTranslExtRef = TranslationUtils.generateRedisKey(
        eTranslJointStr, translationObjs.get(0).getSourceLang(), translationObjs.get(0).getTargetLang(), "et:");

    //create and send the eTransl request
    //baseUrl is different for the integration tests, where the eTranslation service will not be called
    
    
    if(! baseUrlTests.equals(baseUrl)) {
      try {
//        String body = createTranslationBodyAsHtmlDocument(eTranslJointStr,translationObjs.get(0).getSourceLang(),translationObjs.get(0).getTargetLang(),eTranslExtRef);
        String body = createTranslationBodyWithPlainText(eTranslJointStr,translationObjs.get(0).getSourceLang(),translationObjs.get(0).getTargetLang(),eTranslExtRef);
        createHttpRequest(body);
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
              LOGGER.debug("eTranslation response has not been received after waiting for: {} milliseconds.", maxWaitMillisec);
            }
            break;
          }
          long wakeUpTimeNanosec = System.nanoTime();
          sleepTimeMillisec += (wakeUpTimeNanosec-goSleepTimeNanosec)/1000000.0;
        } catch (InterruptedException e) {
        }
      }
      //one last try after the timeout
      String response=redisMessageListener.getMessage();
      if(response!=null) {
        //message received, populate the translations
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug("Received message from redis message listener is: {}", response);
        }
        
        if(response.contains(ETranslationTranslationService.eTranslationErrorCallbackIndicator)) {
          //eTtransl error callback received
          throw new TranslationException(response);
        }
        
        extractTranslationsFromETranslationResponse(translationObjs, redisMessageListenerAdapter, response);   
      }
      /* unsubscibe this listener which automatically deletes the created pub/sub channel,
       * which also gets deleted if the app is stopped or anyhow broken.
       */
      redisMessageListenerContainer.removeMessageListener(redisMessageListenerAdapter);
    }
  }
  
  /**
   * This method extracts the translations from the eTransl html response
   * (the request is sent as an html base64 encoded document).
   * @param translationObjs
   * @param response
   * @throws TranslationException 
   */
  /*
  private void extractTranslationsFromETranslationHtmlResponse(List<TranslationObj> translationObjs, MessageListenerAdapter redisMessageListenerAdapter, String response) throws TranslationException {
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
  */

  private void extractTranslationsFromETranslationResponse(List<TranslationObj> translationObjs, MessageListenerAdapter redisMessageListenerAdapter, String response) throws TranslationException {
    String[] translations=response.split(markupDelimiterWithoutNewline);
    if(translations.length != translationObjs.size()) {
      redisMessageListenerContainer.removeMessageListener(redisMessageListenerAdapter);
      throw new TranslationException("The eTranslation response and the input texts have different size.");
    }
    for(int i=0;i<translations.length;i++) {
      translationObjs.get(i).setTranslation(translations[i].strip());
    }
  }

  /**
   * Generate one eTransl html string to be sent for the translation, as a combination of all input texts.
   * This way the eTransl translates it as a document. 
   * @throws TranslationException 
   */
  /*
  private String generateJointHtmlForTranslation(List<TranslationObj> translationObjs) throws TranslationException {
    StringBuilder translJointString=new StringBuilder(TranslationUtils.STRING_BUILDER_INIT_SIZE);
    translJointString.append("<!DOCTYPE html>\n<htlm>\n<body>\n");
    for(TranslationObj translObj : translationObjs) {
      translJointString.append("<p>");
      translJointString.append(translObj.getText());
      translJointString.append("</p>\n");
    }
    translJointString.append("</body>\n</html>");

    return translJointString.toString();
    
  }
  */
  
  private String generateJointStringForTranslation(List<TranslationObj> translationObjs) {
    StringBuilder translJointString=new StringBuilder(TranslationUtils.STRING_BUILDER_INIT_SIZE);
    for(int i=0;i<translationObjs.size();i++) {
      translJointString.append(translationObjs.get(i).getText());
      if(i<translationObjs.size()-1) {
        translJointString.append(markupDelimiter);
      }
    }
    return translJointString.toString();    
  }
  
  private String createTranslationBodyWithPlainText(String text, String sourceLang, String targetLang, String externalReference) throws JSONException {
    JSONObject jsonBody = new JSONObject().put("priority", 0)
            .put("requesterCallback", getTranslatioCallbackUrl())
            .put("errorCallback", getTranslationErrorCallbackUrl())
            .put("externalReference", externalReference)
            .put("callerInformation", new JSONObject().put("application", credentialUsername).put("username", credentialUsername))
            .put("sourceLanguage", sourceLang.toUpperCase(Locale.ENGLISH))
            .put("targetLanguages", new JSONArray().put(0, targetLang.toUpperCase(Locale.ENGLISH)))
            .put("domain", domain)
//          .put("destinations",
//                  new JSONObject().put("httpDestinations", new JSONArray().put(0, "http://<prod_server_ip>/enrichment-web")))
//          .put("documentToTranslateBase64", new JSONObject().put("format", fileFormat).put("content", base64content));
            .put("textToTranslate", text);

    return jsonBody.toString();
}
 
  /**
   * This method creates the translation request body with an html document to translate. 
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
  /*  
  private String createTranslationBodyAsHtmlDocument(String text, String sourceLang, String targetLang, String externalReference) 
      throws JSONException {
    String base64EncodedText=Base64.encodeBase64String(text.getBytes(StandardCharsets.UTF_8));
    JSONObject jsonBody = new JSONObject().put("priority", 0)
//        .put("requesterCallback", callbackUrl)
//        .put("errorCallback", callbackErrorUrl)
        .put("externalReference", externalReference)
        .put("callerInformation", new JSONObject().put("application", credentialUsername).put("username", credentialUsername))
        .put("sourceLanguage", sourceLang.toUpperCase(Locale.ENGLISH))
        .put("targetLanguages", new JSONArray().put(0, targetLang.toUpperCase(Locale.ENGLISH)))
        .put("domain", domain)
        .put("destinations",
            new JSONObject().put("httpDestinations", new JSONArray().put(0, callbackUrl)))
//        .put("textToTranslate", text);
        .put("documentToTranslateBase64",
            new JSONObject().put("content", base64EncodedText).put("format", "html")
            );
    return jsonBody.toString();
  }
  */

  private long createHttpRequest(String content) throws TranslationException, IOException {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(credentialUsername, credentialPwd));
    CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build();
    HttpPost request = new HttpPost(baseUrl);
    StringEntity body = new StringEntity(content, "UTF-8");
    request.addHeader("content-type", "application/json");
    request.setEntity(body);
    
    CloseableHttpResponse response = httpClient.execute(request);
    StatusLine respStatusLine = response.getStatusLine();
    String respBody=EntityUtils.toString(response.getEntity(), "UTF-8");
    
    if(HttpStatus.SC_OK  != respStatusLine.getStatusCode()) {
      throw new TranslationException("The translation request could not be successfully registered. ETranslation response: " + 
       respStatusLine.getStatusCode() + ", response body: " + respBody);
    }  
    
    long requestNumber;
    try{
      requestNumber = Long.parseLong(respBody);
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug("eTranslation request sent with the request-id: {} and body: {}.", requestNumber, content);
      }
      if(requestNumber < 0) {
        throw wrapETranslationErrorResponse(respBody);
      }
    } catch (NumberFormatException e) {
      throw wrapETranslationErrorResponse(respBody);
    }
    
    return requestNumber;
  }

  TranslationException wrapETranslationErrorResponse(String respBody) {
    return new TranslationException("The translation request could not be successfully registered. ETranslation error response: " + respBody);
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
