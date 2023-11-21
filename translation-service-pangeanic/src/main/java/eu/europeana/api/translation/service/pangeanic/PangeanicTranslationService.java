package eu.europeana.api.translation.service.pangeanic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import eu.europeana.api.translation.definitions.language.PangeanicLanguages;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.service.util.LoggingUtils;

/**
 * Service to send data to translate to Pangeanic Translate API V2
 * 
 * @author Srishti Singh
 */
// TODO get api key, for now passed empty
public class PangeanicTranslationService extends AbstractTranslationService {

  private PangeanicLangDetectService langDetectService;

  protected static final Logger LOG = LogManager.getLogger(PangeanicTranslationService.class);
  public final String externalServiceEndpoint;

  protected CloseableHttpClient translateClient;
  private String serviceId;

  public PangeanicTranslationService(String externalServiceEndpoint,
      PangeanicLangDetectService langDetectService) {
    this.externalServiceEndpoint = externalServiceEndpoint;
    this.langDetectService = langDetectService;
    init();
  }


  /**
   * Creates a new client that can send translation requests to Google Cloud Translate. Note that
   * the client needs to be closed when it's not used anymore
   * 
   * @throws IOException when there is a problem retrieving the first token
   * @throws JSONException when there is a problem decoding the received token
   */
  private void init() {
    if(StringUtils.isBlank(getExternalServiceEndPoint())) {
      return;
    }
    
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(PangeanicTranslationUtils.MAX_CONNECTIONS);
    cm.setDefaultMaxPerRoute(PangeanicTranslationUtils.MAX_CONNECTIONS_PER_ROUTE);
    cm.setDefaultSocketConfig(
        SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(3600000).build());
    // SocketConfig socketConfig =
    // SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(3600000).build(); //We need to set
    // socket keep alive
    translateClient = HttpClients.custom().setConnectionManager(cm).build();
    if (LOG.isInfoEnabled()) {
      LOG.info("Pangeanic translation service is initialized with translate Endpoint - {}",
          getExternalServiceEndPoint());
    }
  }

  /**
   * target language should be English for Pangeanic Translations and validate the source language
   * with list of supported languages
   *
   * @param srcLang source language of the data to be translated
   * @param targetLanguage target language in which data has to be translated
   * @return
   */
  @Override
  public boolean isSupported(String srcLang, String targetLanguage) {
    if (srcLang == null) {
      // automatic language detection
      return isTargetSupported(targetLanguage);
    }
    return PangeanicLanguages.isLanguagePairSupported(srcLang, targetLanguage);
  }

  private boolean isTargetSupported(String targetLanguage) {
    return PangeanicLanguages.isTargetLanguageSupported(targetLanguage);
  }

  @Override
  public void translate(List<TranslationObj> translationObjs, boolean detectLanguages) throws TranslationException {
    try {
      if(translationObjs.isEmpty()) return;
      
      if(detectLanguages) {
        detectLanguages(translationObjs);
      }
      
      computeTranslations(translationObjs);
      
    }
    catch (JSONException e) {
      throw new TranslationException("Exception occured during Pangeanic translation!",
          HttpStatus.SC_BAD_GATEWAY, e);
    }
  }

  @Deprecated
  /**
   * Use the method translate(List<TranslationObj> translationObjs).
   */
  @Override
  public List<String> translate(List<String> texts, String targetLanguage)
      throws TranslationException {
    return translate(texts, targetLanguage, null);
  }

  private void computeTranslations(List<TranslationObj> translationObjs) throws JSONException, TranslationException {
    List<String> analyzedLangs = new ArrayList<String>();
    for(int i=0;i<translationObjs.size();i++) {
      if(translationObjs.get(i).getTranslation()==null) {
        //take the same lang values and send a translation request with a list of texts belonging to that same lang
        String sourceLang = translationObjs.get(i).getSourceLang();
        List<Integer> translIndexes = new ArrayList<Integer>(); 
        translIndexes.add(i);
        List<String> translTexts = new ArrayList<String>();
        translTexts.add(translationObjs.get(i).getText()); 
        String targetLang = translationObjs.get(i).getTargetLang();
  
        if(sourceLang!=null && !analyzedLangs.contains(sourceLang)) {
          for(int j=i+1;j<translationObjs.size();j++) {
            if(translationObjs.get(j).getTranslation()==null) {
              String nextSourceLang = translationObjs.get(j).getSourceLang();
              if(sourceLang.equals(nextSourceLang)) {
                translIndexes.add(j);
                translTexts.add(translationObjs.get(j).getText());
              }
            }
          }
          analyzedLangs.add(sourceLang);
        }
          
        //send the request
        HttpPost translateRequest = PangeanicTranslationUtils.createTranslateRequest(
            getExternalServiceEndPoint(), translTexts, targetLang, sourceLang, "");
        sendTranslateRequestAndParse(translateRequest, translationObjs, translIndexes, sourceLang);
      }        
    }
  }

  @Override
  public void detectLanguages(List<TranslationObj> translationObjs) throws TranslationException {
    List<Integer> indexesWithoutSourceAndTranslation = IntStream.range(0, translationObjs.size())
        .filter(i -> translationObjs.get(i).getSourceLang()==null && translationObjs.get(i).getTranslation()==null)
        .boxed()
        .collect(Collectors.toList());
    if(indexesWithoutSourceAndTranslation.isEmpty()) {
      return;
    }

    if (langDetectService == null) {
      throw new TranslationException("No langDetectService configured!",
          HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    List<String> texts = indexesWithoutSourceAndTranslation.stream()
        .map(index -> translationObjs.get(index).getText())
        .collect(Collectors.toList());
    List<String> detectedLanguages=null;
    try {
      detectedLanguages = langDetectService.detectLang(texts, null);
    } catch (LanguageDetectionException e) {
      throw new TranslationException("Error when tryng to detect the language of the text input!",
          e.getRemoteStatusCode(), e);
    }
    
    if(detectedLanguages!=null) {
      for(int i=0;i<detectedLanguages.size();i++) {
        translationObjs.get(indexesWithoutSourceAndTranslation.get(i)).setSourceLang(detectedLanguages.get(i));
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Pangeanic detect lang request with hint null is executed. Detected languages are {} ",
            LoggingUtils.sanitizeUserInput(detectedLanguages.toString()));
      }
    }
  }

  private void sendTranslateRequestAndParse(HttpPost post, List<TranslationObj> translationObjs, List<Integer> translIndexes, String sourceLanguage) throws TranslationException {
    // initialize with unknown
    int remoteStatusCode = -1;
    try (CloseableHttpResponse response = translateClient.execute(post)) {
      if (response == null || response.getStatusLine() == null) {
        throw new TranslationException(
            "Invalid reponse received from Pangeanic service, no response or status line available!");
      }

      remoteStatusCode = response.getStatusLine().getStatusCode();
      boolean failedRequest = remoteStatusCode != HttpStatus.SC_OK;
      if (failedRequest) {
        throw new TranslationException(
            "Error from Pangeanic Translation API: " + response.getEntity(), remoteStatusCode);
      } else {
        String json = EntityUtils.toString(response.getEntity());
        JSONObject obj = new JSONObject(json);
        // there are cases where we get an empty response
        if (!obj.has(PangeanicTranslationUtils.TRANSLATIONS)) {
          throw new TranslationException("Pangeanic Translation API returned empty response",
              remoteStatusCode);
        }
        extractTranslations(obj, translationObjs, translIndexes, sourceLanguage, remoteStatusCode);
      }
    } catch (ClientProtocolException e) {
      throw new TranslationException("Remote service invocation error.", remoteStatusCode, e);
    } catch (JSONException | IOException e) {
      throw new TranslationException("Cannot read pangeanic service response.", remoteStatusCode,
          e);
    }
  }

  private void extractTranslations(JSONObject obj, List<TranslationObj> translationObjs, List<Integer> translIndexes, String sourceLanguage, int remoteStatusCode) throws JSONException, TranslationException {
    JSONArray translations = obj.getJSONArray(PangeanicTranslationUtils.TRANSLATIONS);
    if(translations.length()==0) {
      throw new TranslationException("Translation failed (empty list) for source language - "
          + obj.get(PangeanicTranslationUtils.SOURCE_LANG), remoteStatusCode);
    }
    for (int i = 0; i < translations.length(); i++) {
      JSONObject object = (JSONObject) translations.get(i);
      if (hasTranslations(object)) {
        double score = object.getDouble(PangeanicTranslationUtils.TRANSLATE_SCORE);
        // only if score returned by the translation service is greater the threshold value, we
        // will accept the translations
        if (score > PangeanicLanguages.getThresholdForLanguage(sourceLanguage)) {
          translationObjs.get(translIndexes.get(i)).setTranslation(object.getString(PangeanicTranslationUtils.TRANSLATE_TARGET));
        }
      }
    }
  }

  private boolean hasTranslations(JSONObject object) {
    return object.has(PangeanicTranslationUtils.TRANSLATE_SOURCE)
        && object.has(PangeanicTranslationUtils.TRANSLATE_TARGET);
  }


  @Override
  public void close() {
    if (translateClient != null) {
      try {
        this.translateClient.close();
      } catch (RuntimeException | IOException e) {
        LOG.error("Error closing connection to Pangeanic Translation API", e);
      }
    }
  }


  @Override
  public String getExternalServiceEndPoint() {
    return externalServiceEndpoint;
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
  public List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage)
      throws TranslationException {
    return null;
  }

}
