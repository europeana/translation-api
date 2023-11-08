package eu.europeana.api.translation.service.pangeanic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  public List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage)
      throws TranslationException {
    try {
      if (texts.isEmpty()) {
        return new ArrayList<>();
      }

      if (sourceLanguage == null) {
        // In this case source language is the hint. The texts passed will be sent for
        // lang-detection first and later will translated
        return translateWithLangDetect(texts, targetLanguage, sourceLanguage);
      }
      
      //regular invocation of external translation service 
      HttpPost post = PangeanicTranslationUtils.createTranslateRequest(getExternalServiceEndPoint(),
          texts, targetLanguage, sourceLanguage, "");
      return PangeanicTranslationUtils.getResults(texts,
          sendTranslateRequestAndParse(post, sourceLanguage));
    } catch (JSONException e) {
      throw new TranslationException("Exception occured during Pangeanic translation!",
          HttpStatus.SC_BAD_GATEWAY, e);
    }
  }

  @Override
  public List<String> translate(List<String> texts, String targetLanguage)
      throws TranslationException {
    return translate(texts, targetLanguage, null);
  }


  /**
   * Translates the texts with no source language. First a lang detect request is sent to identify
   * the source language Later translations are performed
   *
   * @param texts
   * @param targetLanguage
   * @return
   * @throws TranslationException
   */
  private List<String> translateWithLangDetect(List<String> texts, String targetLanguage,
      String langHint) throws TranslationException {
    try {
      if (langDetectService == null) {
        throw new TranslationException("No langDetectService configured!",
            HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }
      List<String> detectedLanguages = detectLanguages(texts, langHint);
      Map<String, String> translations =
          computeTranslations(texts, targetLanguage, detectedLanguages, langHint);
      return PangeanicTranslationUtils.getResults(texts, translations);
    } catch (JSONException | IOException e) {
      throw new TranslationException("Exception occured during Pangeanic translation!",
          HttpStatus.SC_BAD_GATEWAY, e);
    }
  }


  private Map<String, String> computeTranslations(List<String> texts, String targetLanguage,
      List<String> detectedLanguages, String langHint)
      throws JSONException, IOException, TranslationException {
    // create lang-value map for translation
    Map<String, List<String>> detectedLangValueMap =
        PangeanicTranslationUtils.getDetectedLangValueMap(texts, detectedLanguages);
    
    Map<String, String> translations = new LinkedHashMap<>();
    if (LOG.isDebugEnabled()) {
      LOG.debug(
          "Pangeanic detect lang request with hint {} is executed. Detected languages are {} ",
          LoggingUtils.sanitizeUserInput(langHint),
          LoggingUtils.sanitizeUserInput(detectedLangValueMap.keySet().toString()));
    }
    
    for (Map.Entry<String, List<String>> entry : detectedLangValueMap.entrySet()) {
        HttpPost translateRequest = PangeanicTranslationUtils.createTranslateRequest(
            getExternalServiceEndPoint(), entry.getValue(), targetLanguage, entry.getKey(), "");
        translations.putAll(sendTranslateRequestAndParse(translateRequest, entry.getKey()));   
    }
    return translations;
  }


  private List<String> detectLanguages(List<String> texts, String langHint)
      throws TranslationException {
    List<String> detectedLanguages;
    try {
      detectedLanguages = langDetectService.detectLang(texts, langHint);
    } catch (LanguageDetectionException e) {
      throw new TranslationException("Error when tryng to detect the language of the text input!",
          e.getRemoteStatusCode(), e);
    }
    return detectedLanguages;
  }

  private Map<String, String> sendTranslateRequestAndParse(HttpPost post, String sourceLanguage)
      throws TranslationException {

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
        Map<String, String> results = new LinkedHashMap<>();
        // there are cases where we get an empty response
        if (!obj.has(PangeanicTranslationUtils.TRANSLATIONS)) {
          throw new TranslationException("Pangeanic Translation API returned empty response",
              remoteStatusCode);
        }
        extractTranslations(obj, sourceLanguage, results);
        // response should not be empty
        if (results.isEmpty()) {
          throw new TranslationException("Translation failed for source language - "
              + obj.get(PangeanicTranslationUtils.SOURCE_LANG), remoteStatusCode);
        }
        return results;
      }
    } catch (ClientProtocolException e) {
      throw new TranslationException("Remote service invocation error.", remoteStatusCode, e);
    } catch (JSONException | IOException e) {
      throw new TranslationException("Cannot read pangeanic service response.", remoteStatusCode,
          e);
    }
  }


  private void extractTranslations(JSONObject obj, String sourceLanguage,
      Map<String, String> results) throws JSONException {
    JSONArray translations = obj.getJSONArray(PangeanicTranslationUtils.TRANSLATIONS);
    for (int i = 0; i < translations.length(); i++) {
      JSONObject object = (JSONObject) translations.get(i);
      if (hasTranslations(object)) {
        double score = object.getDouble(PangeanicTranslationUtils.TRANSLATE_SCORE);
        // only if score returned by the translation service is greater the threshold value, we
        // will accept the translations
        if (score > PangeanicLanguages.getThresholdForLanguage(sourceLanguage)) {
          results.put(object.getString(PangeanicTranslationUtils.TRANSLATE_SOURCE),
              object.getString(PangeanicTranslationUtils.TRANSLATE_TARGET));
        } else {
          // for discarded thresholds add null as translations values
          results.put(object.getString(PangeanicTranslationUtils.TRANSLATE_SOURCE), null);
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

}
