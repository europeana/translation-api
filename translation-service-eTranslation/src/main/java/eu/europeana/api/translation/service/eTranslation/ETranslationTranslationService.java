package eu.europeana.api.translation.service.eTranslation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
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
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.service.util.GeneralUtils;

public class ETranslationTranslationService extends AbstractTranslationService {
  
  protected static final Logger logger = LogManager.getLogger(ETranslationTranslationService.class);

  private String serviceId;
  private String baseUrl;
  private String domain;
  //this is the base url of the translation api (without the request handler (or the controller) endpoint)
  private String callbackUrl;
  private String credentialUsername;
  private String credentialPwd;
  private int maxWaitMillisec;
  private static long externalReferenceCounter;
	
  /**
   * This is a Map that represents the created requests for translation. For each request, 
   * the key is an "external-reference" (see eTranslation documentation), which is used as a unique identifier
   * of the text to be translated, and the value is the translated text. This map is used to check if the 
   * translation has been completed (over asynchronous calls) in which case the map should contain a NOT null
   * value for the given key.
   */	
  private static Map<String, String> createdRequests = new HashMap<String, String>();
  private static Map<String, String> createdRequestsSynchronized = Collections.synchronizedMap(createdRequests);
	
  public ETranslationTranslationService(String baseUrl, String domain, String callbackUrl, int maxWaitMillisec, String credentialsFilePath) throws Exception {
    this.baseUrl = baseUrl;
    this.domain = domain;
    this.callbackUrl=callbackUrl;
    this.maxWaitMillisec=maxWaitMillisec;
    externalReferenceCounter=0;
    if(!StringUtils.isBlank(credentialsFilePath)) {
      readCredentialsFile(credentialsFilePath);
    }
  }
	
  /**
   * Reading the eTranslation credentials 
   * 
   * @param path		the absolute path to the credential file
   * @throws IOException 
   * @throws Exception 
   */
  private void readCredentialsFile(String path) throws IOException  {	
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] splitString = GeneralUtils.toArray(line,"=");
        if (splitString[0].equals("user"))
          credentialUsername = splitString[1];
        else if (splitString[0].equals("pwd"))
          credentialPwd = splitString[1];
      }
    } catch (IOException e) {
	    throw e;
    }
  }

  @Override
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {    
    List<String> externalRefsForCallbacks = new ArrayList<>();//this variable below captures only the external references of successfully created requests
    List<String> externalRefsAll = new ArrayList<>();
    List<Integer> indexesTranslated = new ArrayList<>();
    //create eTransaltion requests and send them
    try {
      for(int i=0;i<translationObjs.size();i++) {
        String sourceLang = translationObjs.get(i).getSourceLang();
        String targetLang = translationObjs.get(i).getTargetLang();
        String text = translationObjs.get(i).getText();
        if(sourceLang!=null && targetLang!=null && !StringUtils.isBlank(text)) {
          String extRef = generateExternalReferenceAndSaveToMap();
          externalRefsAll.add(extRef);
          String reponseCode=null;
          if(!StringUtils.isBlank(baseUrl)) {
            String requstBody = createTranslationBody(text,sourceLang,targetLang,extRef);
            /*
             * For simulating the eTranslation callbacks for the local testing, please use the method 
             * createHttpRequestSimulator(extRef), instead of the createHttpRequest(requestBody), like this:
             * String reponseCode = createHttpRequestSimulator(extRef, text);
             * and comment out the last catch block (catch (IOException e)). The simulator method executes 
             * in a new thread and calls back the same method as the eTranslation callback.
             */
            reponseCode = createHttpRequest(requstBody);
          }
          else {
            reponseCode = createHttpRequestSimulator(extRef, text);
          }
           
          //in case of the successfull request, the return code number is positive, otherwise negative  
          if(Integer.valueOf(reponseCode) >= 0) {
            externalRefsForCallbacks.add(extRef);
            indexesTranslated.add(i);
          }
          if(logger.isDebugEnabled()) {
            logger.debug("Sent eTranslation request. Response code: " + reponseCode + ". External reference: " + extRef);
          }
        }
      }
    } 
    catch (JSONException e) {
      clearRequestsMapForExternalReferences(externalRefsAll);
      throw new TranslationException("Exception during the eTranslation request body creation.");
    } 
    catch (IOException e) {
      clearRequestsMapForExternalReferences(externalRefsAll);
      throw new TranslationException("Exception during the eTranslation http request sending.");
    }
    
    //waiting for the callbacks
    if(externalRefsForCallbacks.size()>0) {
      long waitingTime = 0;
      long sleepingTimeMillisec = 500;
      while(!allCallbacksReceived(externalRefsForCallbacks) && waitingTime < maxWaitMillisec)
      {
          try {
            Thread.sleep(sleepingTimeMillisec);
          } catch (InterruptedException e) {
          }
          waitingTime += sleepingTimeMillisec;
      }
      
      if(waitingTime >= maxWaitMillisec)
      {
        if(logger.isInfoEnabled()) {
          logger.info("Maximum waiting time of: " + String.valueOf(maxWaitMillisec) + " for the eTranslation callback has elapsed!");
        }
      }
      
      //populate the responses
      for(int index : indexesTranslated) {
        translationObjs.get(index).setTranslation(createdRequestsSynchronized.get(externalRefsForCallbacks.get(index)));
      }
    }

    //clear all external references added to the map
	clearRequestsMapForExternalReferences(externalRefsAll);
		
  }
  
  private void clearRequestsMapForExternalReferences(List<String> externalRefs) {
    for(String extRef : externalRefs) {
      createdRequestsSynchronized.remove(extRef);
    }
  }
  
  private boolean allCallbacksReceived(List<String> externalRefs) {
    for(String extRef : externalRefs) {
      if(createdRequestsSynchronized.get(extRef)==null) {
        return false;
      }
    }
    return true;
  }

  /**
   * The generation of the external references is synchronized among all requests,
   * so that each translation request (a single request to the eTranslation service)
   * receives a unique external reference number as string.
   * 
   * @return
   */
  private static synchronized String generateExternalReferenceAndSaveToMap() {
    while(true) {
      externalReferenceCounter = externalReferenceCounter + 1;
      String extRefString = String.valueOf(externalReferenceCounter);
      if(! createdRequestsSynchronized.containsKey(extRefString)) {
        createdRequestsSynchronized.put(extRefString, null);
        return extRefString;
      }
    }
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
   */
  private String createTranslationBody(String text, String sourceLang, String targetLang, String externalReference) throws JSONException {
    JSONObject jsonBody = new JSONObject().put("priority", 0)
        .put("requesterCallback", callbackUrl)
        .put("externalReference", externalReference)
        .put("callerInformation", new JSONObject().put("application", credentialUsername).put("username", credentialUsername))
        .put("sourceLanguage", sourceLang.toUpperCase())
        .put("targetLanguages", new JSONArray().put(0, targetLang.toUpperCase()))
        .put("domain", domain)
//        .put("destinations",
//            new JSONObject().put("httpDestinations", new JSONArray().put(0, callbackUrlBase)))
        .put("textToTranslate", text);
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
    CloseableHttpResponse result = httpClient.execute(request);
    String responeString = EntityUtils.toString(result.getEntity(), "UTF-8");
    return responeString;
  }
  
  class eTranslationSimulatorThread implements Runnable {
    private String extRef;
    private String translation;
    public eTranslationSimulatorThread(String extRef, String translation) {
      this.extRef = extRef;
      this.translation=translation;
    }
    @Override
    public void run() {
      try {
        Thread.sleep(1000);
        processCallback(null, translation, null, extRef);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  private String createHttpRequestSimulator(String externalReference, String translation) {
    Thread thread = new Thread(new eTranslationSimulatorThread(externalReference, translation));
    thread.start();
    return "1";//it should return the positive number as string
  }
		
  public void processCallback(String targetLanguage, String translatedText, String requestId, String externalReference) {
    if(logger.isDebugEnabled()) {
      logger.debug("eTranslation response has been received with the following parameters: targetLanguage="+ targetLanguage + ", translatedText="+ translatedText + ", requestId=" + requestId + ", externalReference="+externalReference);
    }
   
    if(createdRequestsSynchronized.containsKey(externalReference))
    {
      createdRequestsSynchronized.put(externalReference, translatedText);
    }
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
