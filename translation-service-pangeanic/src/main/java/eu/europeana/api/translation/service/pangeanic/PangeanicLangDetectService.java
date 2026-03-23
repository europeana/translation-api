package eu.europeana.api.translation.service.pangeanic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.AbstractLanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Language detection service based on Pangeanic solution
 */
public class PangeanicLangDetectService extends AbstractLanguageDetectionService{

  protected static final Logger LOG = LogManager.getLogger(PangeanicLangDetectService.class);
  private static final double THRESHOLD = 0.5;
  private final String externalServiceEndpoint;
  
  private Set<String> supportedLanguages = Set.of("sk", "ro", "bg", "pl", "hr", "sv", "fr", "it",
      "es", "cs", "de", "lv", "nl", "el", "fi", "da", "sl", "hu", "pt", "et", "lt", "ga", "en");

  protected CloseableHttpClient detectClient;

  public PangeanicLangDetectService(String endPoint) {
    this.externalServiceEndpoint = endPoint;
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
    int keepAliveTimeut = 60;
    cm.setDefaultSocketConfig(
        SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(Timeout.of(keepAliveTimeut, TimeUnit.MINUTES)).build());
    detectClient = HttpClients.custom().setConnectionManager(cm).build();
    if(LOG.isInfoEnabled()) {
      LOG.info(
          "Pangeanic Language Detection service is initialized with detect language Endpoint - {}",
        getExternalServiceEndPoint());
    }
  }
  
  @Override
  public boolean isSupported(String srcLang) {
    return supportedLanguages.contains(srcLang);
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs)
      throws LanguageDetectionException {
    if (languageDetectionObjs.isEmpty()) {
      return;
    }

    // get values for the request
    List<String> texts = new ArrayList<>();
    languageDetectionObjs.stream().forEach(obj -> texts.add(obj.getText()));

    String langHint = languageDetectionObjs.get(0).getHint();

    HttpPost post = PangeanicTranslationUtils
        .createDetectlanguageRequest(getExternalServiceEndPoint(), texts, langHint, "");
    List<String> results = sendDetectRequestAndParse(post);

    // fallback check - if the lang detection is complete / successful
    if (results.size() != languageDetectionObjs.size()) {
      throw new LanguageDetectionException("The Language detection is not completed successfully. Expected "
              + languageDetectionObjs.size() + " but received: " + results.size());
    }

    // build results
    for(int i=0; i< results.size(); i++) {
      languageDetectionObjs.get(i).setDetectedLang(results.get(i));
    }
  }

  /**
   * Send the request to Pangeanic and parses the response in the list of strings NOTE : We do not
   * accept results if the threshold is lower than 0.5 For anything not recognised or present or not
   * acceptable , we add null values in the list
   *
   * @param post http post request with body
   * @return list of languages detected in the same sequence
   * @throws IOException
   * @throws JSONException
   * @throws LanguageDetectionException
   */
  private List<String> sendDetectRequestAndParse(HttpPost post) throws LanguageDetectionException {
    //initialize with unknown
    int remoteStatusCode = -1;
    try (CloseableHttpResponse response = detectClient.execute(post)) {
      // Pageanic BUG - sometimes language detect sends 400 Bad request with proper response and
      // error message
      if (response == null) {
        throw new LanguageDetectionException(
            "Invalid reponse received from Pangeanic service, no response or status line available!");
      } 
      
      remoteStatusCode = response.getCode(); 
      boolean failedRequest = remoteStatusCode != HttpStatus.SC_OK;
      String json = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
      if ( failedRequest ) {
        throw new LanguageDetectionException(
            "Error from Pangeanic Language Detect API: " + json,
            remoteStatusCode);
      } else {
        // sometimes language detect sends 200 ok status with empty response data
        if (json.isEmpty()) {
          throw new LanguageDetectionException("Language detect returned an empty response",
              remoteStatusCode);
        }
        JSONObject obj = new JSONObject(json);

        // if json doesn't have detected lanaguge throw a error
        if (!obj.has(PangeanicTranslationUtils.DETECTED_LANGUAGE)) {
          throw new LanguageDetectionException(
              "Language detect response doesn't have detected_langs tags",
              remoteStatusCode);
        }

        return extractDetectedLanguages(obj);
      }
    } catch (ClientProtocolException e) {
      throw new LanguageDetectionException("Remote service invocation error.",
          remoteStatusCode, e);
    } catch (JSONException | ParseException | IOException e) {
      throw new LanguageDetectionException("Cannot read pangeanic service response.",
          remoteStatusCode, e);
    } 
  }

  private List<String> extractDetectedLanguages(JSONObject obj) throws JSONException {
    List<String> result = new ArrayList<>();
    JSONArray detectedLangs = obj.getJSONArray(PangeanicTranslationUtils.DETECTED_LANGUAGE);
    for (int i = 0; i < detectedLangs.length(); i++) {
      JSONObject object = (JSONObject) detectedLangs.get(i);
      if (hasLanguageAndScoreDetected(object)) {
        double langScore = object.getDouble(PangeanicTranslationUtils.SOURCE_LANG_SCORE);
        // if lang detected is lower than 0.5 score then don't accept the results
        if (langScore >= THRESHOLD) {
          result.add(object.getString(PangeanicTranslationUtils.SOURCE_DETECTED));
        } else {
          result.add(null);
        }
      } else {
        // when no detected lang is returned. Ideally, this should not happen
        // But there are time Pangeanic returns no src_detected value
        // These values as well will remain non-translated
        result.add(null);
      }
    }
    return result;
  }

  private boolean hasLanguageAndScoreDetected(JSONObject object) {
    return object.has(PangeanicTranslationUtils.SOURCE_DETECTED)
        && object.has(PangeanicTranslationUtils.SOURCE_LANG_SCORE);
  }

  @Override
  public void close() {
    if (detectClient != null) {
      try {
        this.detectClient.close();
      } catch (IOException e) {
        LOG.error("Error closing connection to Pangeanic Translation API", e);
      }
    }
  }

  @Override
  public String getExternalServiceEndPoint() {
    return externalServiceEndpoint;
  }
}
