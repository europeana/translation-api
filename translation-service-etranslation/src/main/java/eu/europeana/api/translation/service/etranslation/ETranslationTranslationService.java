package eu.europeana.api.translation.service.etranslation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
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
  public static final String DEFAULT_SERVICE_ID = "ETRANSLATION";
  public static final String FAKE_BASE_URL_FOR_TESTING = "base-url-for-testing";
  // base64 encoded string (as in generateRedisKey()) within the [notranslate] tag
  public static final String MARKUP_DELIMITER = "\n[notranslate]deenPVsaOg[/notranslate]\n";
  public static final String MARKUP_DELIMITER_WITHOUT_NEWLINE =
      Pattern.quote("[notranslate]deenPVsaOg[/notranslate]");
  public static final String ERROR_CALLBACK_MARKUP = "eTranslationErrorCallback";
  public static final String PATH_CALLBACK = "/etranslation/callback";
  public static final String PATH_ERROR_CALLBACK = "/etranslation/error-callback";
  public static final int ETRANSLATION_SNIPPET_LIMIT = 4990;
  public static final int ETRANSLATION_SNIPPET_LIMIT_TESTS = 200;
  private static final int SECOND_MILIS = 1000;
  
  
  private String serviceId;
  private String baseUrl;
  private String domain;
  private String translationApiBaseUrl;
  private String credentialUsername;
  private String credentialPwd;
  private int maxWaitMillisec;
  private RedisMessageListenerContainer redisMessageListenerContainer;

  /**
   * Default constructor without service initialization, see {@link #init(String, String, String, int, String, String, RedisMessageListenerContainer)}
   */
  public ETranslationTranslationService() {
    super();
  }
  
  /**
   * Contructor for etranslation service with initialization
   * @param etranslationServiceBaseUrl base uRL of eTranslation service
   * @param domain eTranslation domain
   * @param translationApiBaseUrl the base URL of the translation API deployment
   * @param maxWaitMillisec timeout for eTranslation callback 
   * @param username eTranslation credential
   * @param password eTranslation credential
   * @param redisMessageListenerContainer container for PUB/SUB redis message listeners
   * @throws TranslationException thrown in case that the translation cannot be performed/retrieved 
   */
  public ETranslationTranslationService(String etranslationServiceBaseUrl, String domain,
      String translationApiBaseUrl, int maxWaitMillisec, String username, String password,
      RedisMessageListenerContainer redisMessageListenerContainer) throws TranslationException {
    init(etranslationServiceBaseUrl, domain, translationApiBaseUrl, maxWaitMillisec, username,
        password, redisMessageListenerContainer);
  }

  /**
   * Method for service initialization
   * @param etranslationServiceBaseUrl base uRL of eTranslation service
   * @param domain eTranslation domain
   * @param translationApiBaseUrl the base URL of the translation API deployment
   * @param maxWaitMillisec timeout for eTranslation callback 
   * @param username eTranslation credential
   * @param password eTranslation credential
   * @param redisMessageListenerContainer container for PUB/SUB redis message listeners
   * @throws TranslationException thrown in case that the translation cannot be performed/retrieved 
   */
  public void init(String etranslationServiceBaseUrl, String domain, String translationApiBaseUrl,
      int maxWaitMillisec, String username, String password,
      RedisMessageListenerContainer redisMessageListenerContainer) throws TranslationException {
    if (!FAKE_BASE_URL_FOR_TESTING.equals(etranslationServiceBaseUrl)) {
      validateETranslConfigParams(etranslationServiceBaseUrl, domain, translationApiBaseUrl,
          maxWaitMillisec, username, password);
    }
    this.baseUrl = etranslationServiceBaseUrl;
    this.translationApiBaseUrl = translationApiBaseUrl;
    this.domain = domain;
    this.maxWaitMillisec = maxWaitMillisec;
    this.credentialUsername = username;
    this.credentialPwd = password;
    this.redisMessageListenerContainer = redisMessageListenerContainer;
  }

  private String getTranslationErrorCallbackUrl() {
    return this.translationApiBaseUrl + PATH_ERROR_CALLBACK;
  }

  private String getTranslatioCallbackUrl() {
    return this.translationApiBaseUrl + PATH_CALLBACK;
  }

  private void validateETranslConfigParams(String etranslationServiceBaseUrl, String domain,
      String translationApiBaseUrl, int maxWaitMillisec, String username, String password)
      throws TranslationException {
    List<String> missingParams = new ArrayList<>(6);
    if (StringUtils.isBlank(etranslationServiceBaseUrl)) {
      missingParams.add("baseUrl");
    }
    if (StringUtils.isBlank(domain)) {
      missingParams.add("domain");
    }
    if (StringUtils.isBlank(translationApiBaseUrl)) {
      missingParams.add("translationApiBaseUrl");
    }
    if (maxWaitMillisec <= 0) {
      missingParams.add("maxWaitMillisec (must be >0)");
    }
    if (StringUtils.isBlank(username)) {
      missingParams.add("username");
    }
    if (StringUtils.isBlank(password)) {
      missingParams.add("password");
    }

    if (!missingParams.isEmpty()) {
      throw new TranslationException(
          "Invalid eTranslation config parameters: " + missingParams.toString());
    }
  }

  @Override
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    String sourceLang = translationObjs.get(0).getSourceLang();
    if (sourceLang == null) {
      throw new TranslationException(
          "The source language cannot be null for the eTranslation service.");
    }
    String eTranslJointStr = generateJointStringForTranslation(translationObjs);
    MessageListenerAdapter redisMessageListenerAdapter = null;

    /*
     * create an eTransl request with an external reference and send it. The same external reference
     * is received in the eTransl callback. That reference is used for the name of the channel for
     * the redis message subscriber listener created below, which will be notified from the redis
     * publisher after the eTransl callback comes. The publisher will publish to the same channel
     * using the external reference from the eTransl callback.
     */
    // create external reference for eTransl service
    String eTranslExtRef = TranslationUtils.generateRedisKey(eTranslJointStr,
        translationObjs.get(0).getSourceLang(), translationObjs.get(0).getTargetLang(), "et:");

    try {
      //create request body
      String body =
          createTranslationRequestBody(eTranslJointStr, translationObjs.get(0).getSourceLang(),
              translationObjs.get(0).getTargetLang(), eTranslExtRef);

      // register redis channel
      redisMessageListenerAdapter = registerRedisChannel(eTranslExtRef, eTranslJointStr.length());

      //send the eTransl request, if not fake url
      if (!FAKE_BASE_URL_FOR_TESTING.equals(baseUrl)) {
        sendTranslationRequest(body);
      }
      // read translation response
      readTranslationResponseFromRedis(redisMessageListenerAdapter, translationObjs, eTranslJointStr.length());
      
    } catch (JSONException e) {
      throw new TranslationException(
          "Exception during the eTranslation http request body creation.", 0, e);
    } finally {
      /*
       * unsubscibe this listener which automatically deletes the created pub/sub channel, which
       * also gets deleted if the app is stopped or anyhow broken.
       */
      if(redisMessageListenerAdapter != null) {
        redisMessageListenerContainer.removeMessageListener(redisMessageListenerAdapter);
      }
    }
  }

 
  private void readTranslationResponseFromRedis(MessageListenerAdapter redisMessageListenerAdapter, List<TranslationObj> translationObjs,
      int textSize) throws TranslationException {
    
    if(redisMessageListenerAdapter == null) {
      //if channel not registered, cannot read results
      return;
    }
    
    String response =
        readMessageFromChannel(redisMessageListenerAdapter);

    if (response == null) {
      throw new TranslationException(
          "No response received from external eTranslation Service within expected interval of seconds: "
              + toSeconds(maxWaitMillisec),
          HttpStatus.SC_GATEWAY_TIMEOUT);
    }

    // message received, populate the translations
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Received message from redis message listener is: {}", response);
    }

    if (response.contains(ETranslationTranslationService.ERROR_CALLBACK_MARKUP)) {
      // eTtransl error callback received
      throw new TranslationException(response, HttpStatus.SC_UNPROCESSABLE_CONTENT);
    }

    // fill translations into the provided translation list
    fillTranslationsFromRemoteResponse(translationObjs, response, isSnippetLimitExceeded(textSize));
  }

  private int toSeconds(int maxWaitMillisec) {
    return maxWaitMillisec / SECOND_MILIS;
  }

  private boolean isSnippetLimitExceeded(int textSize) {
    // use smaller limit for the tests (e.g. 200)
    if (FAKE_BASE_URL_FOR_TESTING.equals(baseUrl)) {
      return (textSize > ETRANSLATION_SNIPPET_LIMIT_TESTS);
    } else {
      return (textSize > ETRANSLATION_SNIPPET_LIMIT);
    }
  }

  private String readMessageFromChannel(MessageListenerAdapter redisMessageListenerAdapter){
    
    RedisMessageListener redisMessageListener =
        (RedisMessageListener) redisMessageListenerAdapter.getDelegate();
   
    if(redisMessageListener == null) {
      throw new IllegalArgumentException("ETranslation callback handling was not propetly initilized, the message listener must not be null!");
    }
    
    
    synchronized (redisMessageListener) {
      /*
       * While loop as a good practice to ensure spurious wake-ups
       * (https://www.baeldung.com/java-wait-notify). In addition, time is measured to not wait
       * again and again the same max time, in case of spurious wake-ups
       */
      long sleepTimeMillisec = 0;
      while (redisMessageListener.getMessage() == null) {
        try {
          long goSleepTimeNanosec = System.nanoTime();
          if (sleepTimeMillisec < maxWaitMillisec) {
            //wait for redis notification
            redisMessageListener.wait(maxWaitMillisec - sleepTimeMillisec);
          } else {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "eTranslation response has not been received after waiting for: {} milliseconds.",
                  maxWaitMillisec);
            }
            //timeout expired, stop the sleep loop
            break;
          }
          //send back to sleep if the eTranslation response (message) was not received yet  
          long wakeUpTimeNanosec = System.nanoTime();
          sleepTimeMillisec += (wakeUpTimeNanosec - goSleepTimeNanosec) / 1000000.0;
        } catch (InterruptedException e) {
          //log interruption exceptions
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Redis Message Listener Interruption exception!", e);
          }
        }
      }

      // get translation response from message listener
      return redisMessageListener.getMessage();
    }
  }

  private MessageListenerAdapter registerRedisChannel(String eTranslExtRef, int textSize) {
    boolean requestAsDocument = isSnippetLimitExceeded(textSize);
    MessageListenerAdapter redisMessageListenerAdapter =
        new MessageListenerAdapter(new RedisMessageListener(requestAsDocument));
    redisMessageListenerContainer.addMessageListener(redisMessageListenerAdapter,
        ChannelTopic.of(eTranslExtRef));
    return redisMessageListenerAdapter;
  }

  private void fillTranslationsFromRemoteResponse(List<TranslationObj> translationObjs,
      String response, boolean responseAsDocument) throws TranslationException {
    if (responseAsDocument) {
      fillTranslationsFromDocumentTranslationResponse(translationObjs, response);
    } else {
      fillTranslationsFromTextSnippetTranslationResponse(translationObjs, response);
    }
  }


  /**
   * This method extracts the translations from the eTransl txt document response (the request is
   * sent as an txt base64 encoded document).
   * 
   * @param translationObjs
   * @param redisMessageListenerAdapter
   * @param response
   * @throws TranslationException
   */
  private void fillTranslationsFromDocumentTranslationResponse(List<TranslationObj> translationObjs,
      String response) throws TranslationException {
    // first base64 decode
    String respBase64Decoded = new String(Base64.decodeBase64(response), StandardCharsets.UTF_8);
    fillTranslationsFromTextSnippetTranslationResponse(translationObjs, respBase64Decoded);
  }

  /**
   * Extracts the translations in case the text snippet is sent in the request (as a
   * text-to-translate parameter)
   * 
   * @param translationObjs
   * @param redisMessageListenerAdapter
   * @param response
   * @throws TranslationException
   */

  private void fillTranslationsFromTextSnippetTranslationResponse(
      List<TranslationObj> translationObjs, String response) throws TranslationException {
    String[] translations = response.split(MARKUP_DELIMITER_WITHOUT_NEWLINE);
    if (translations.length != translationObjs.size()) {
      throw new TranslationException(
          "The eTranslation response and the input texts have different size.");
    }
    for (int i = 0; i < translations.length; i++) {
      translationObjs.get(i).setTranslation(translations[i].strip());
    }
  }


  private String generateJointStringForTranslation(List<TranslationObj> translationObjs) {

    List<String> texts = translationObjs.stream().map(to -> to.getText()).toList();
    return String.join(MARKUP_DELIMITER, texts);
  }

  private String createTranslationRequestBody(String text, String sourceLang, String targetLang,
      String externalReference) throws JSONException {
    if (isSnippetLimitExceeded(text.length())) {
      return createTranslationBodyWithDocument(text, sourceLang, targetLang, externalReference);
    } else {
      return createTranslationBodyWithTextSnippet(text, sourceLang, targetLang, externalReference);
    }
  }

  /**
   * Creates a request with a text-snippet to translate (no document to be sent).
   * 
   * @param text
   * @param sourceLang
   * @param targetLang
   * @param externalReference
   * @return
   * @throws JSONException
   */
  private String createTranslationBodyWithTextSnippet(String text, String sourceLang,
      String targetLang, String externalReference) throws JSONException {
    JSONObject jsonBody = new JSONObject().put("priority", 0)
        .put("requesterCallback", getTranslatioCallbackUrl())
        .put("errorCallback", getTranslationErrorCallbackUrl())
        .put("externalReference", externalReference)
        .put("callerInformation",
            new JSONObject().put("application", credentialUsername).put("username",
                credentialUsername))
        .put("sourceLanguage", sourceLang.toUpperCase(Locale.ENGLISH))
        .put("targetLanguages", new JSONArray().put(0, targetLang.toUpperCase(Locale.ENGLISH)))
        .put("domain", domain)
        .put("textToTranslate", text);

    return jsonBody.toString();
  }

  /**
   * This method creates the translation request body with a document to translate. The response is
   * sent back to the application over a specified callback URL (REST service).
   * 
   * @param text
   * @param sourceLang
   * @param targetLang
   * @param externalReference
   * @return
   * @throws JSONException
   * @throws UnsupportedEncodingException
   */
  private String createTranslationBodyWithDocument(String text, String sourceLang,
      String targetLang, String externalReference) throws JSONException {
    String base64EncodedText = Base64.encodeBase64String(text.getBytes(StandardCharsets.UTF_8));
    JSONObject jsonBody = new JSONObject().put("priority", 0)
        .put("requesterCallback", getTranslatioCallbackUrl())
        .put("errorCallback", getTranslationErrorCallbackUrl())
        .put("externalReference", externalReference)
        .put("callerInformation",
            new JSONObject().put("application", credentialUsername).put("username",
                credentialUsername))
        .put("sourceLanguage", sourceLang.toUpperCase(Locale.ENGLISH))
        .put("targetLanguages", new JSONArray().put(0, targetLang.toUpperCase(Locale.ENGLISH)))
        .put("domain", domain)
        .put("destinations",
            new JSONObject().put("httpDestinations",
                new JSONArray().put(0, getTranslatioCallbackUrl())))
        // .put("textToTranslate", text);
        .put("documentToTranslateBase64",
            new JSONObject().put("content", base64EncodedText).put("format", "txt"));
    return jsonBody.toString();
  }

  private long sendTranslationRequest(String content) throws TranslationException{
    CredentialsStore credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials( new AuthScope(baseUrl, -1),
        new UsernamePasswordCredentials(credentialUsername, credentialPwd.toCharArray()));
    CloseableHttpClient httpClient =
        HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build();
    
    HttpPost request = new HttpPost(baseUrl);
    StringEntity body = new StringEntity(content, StandardCharsets.UTF_8);
    request.addHeader("content-type", ContentType.APPLICATION_JSON.getMimeType());
    request.setEntity(body);
    
    BasicHttpClientResponseHandler responseHandler = new BasicHttpClientResponseHandler();
    
    String responseBody; 
    try {
      responseBody = httpClient.execute(request, responseHandler);
     }catch (IOException e) {
       throw new TranslationException(
           "The translation request could not be successfully registered. ETranslation response: " + e.getMessage());
    } 
   
    long requestNumber;
    try {
      requestNumber = Long.parseLong(responseBody);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("eTranslation request sent with the request-id: {} and body: {}.",
            requestNumber, sanitizeRequestBodyForLogging(content));
      }
      if (requestNumber < 0) {
        throw wrapETranslationErrorResponse(responseBody);
      }
    } catch (NumberFormatException e) {
      throw wrapETranslationErrorResponse(responseBody);
    }

    return requestNumber;
  }

  private String sanitizeRequestBodyForLogging(String content) {
    if(content != null) {
      return content.replace(getCredentialUsername(), "*****")
          .replace(getCredentialPwd(), "*****");
      
    }
    return content;
  }

  TranslationException wrapETranslationErrorResponse(String respBody) {
    return new TranslationException(
        "The translation request could not be successfully registered. ETranslation error response: "
            + respBody);
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
    //nothing to do here
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

  private String getCredentialUsername() {
    return credentialUsername;
  }

  private String getCredentialPwd() {
    return credentialPwd;
  }

}
