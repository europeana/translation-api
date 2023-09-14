package eu.europeana.api.translation.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpStatus;
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
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

public class PangeanicLangDetectService implements LanguageDetectionService {
  
  protected static final Logger LOG = LogManager.getLogger(PangeanicLangDetectService.class);
  private static final double THRESHOLD = 0.5;
  private final String externalServiceEndpoint;
  private String serviceId;

    protected CloseableHttpClient detectClient;

    public PangeanicLangDetectService(String endPoint) {
      this.externalServiceEndpoint = endPoint;
      init();
      
    }
    
    /**
     * Creates a new client that can send translation requests to Google Cloud Translate. Note that the client needs
     * to be closed when it's not used anymore
     * @throws IOException when there is a problem retrieving the first token
     * @throws JSONException when there is a problem decoding the received token
     */
    private void init() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(PangeanicTranslationUtils.MAX_CONNECTIONS);
        cm.setDefaultMaxPerRoute(PangeanicTranslationUtils.MAX_CONNECTIONS_PER_ROUTE);
        cm.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(3600000).build());
//        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(3600000).build(); //We need to set socket keep alive
        detectClient = HttpClients.custom().setConnectionManager(cm).build();
        LOG.info("Pangeanic Language Detection service is initialized with detect language Endpoint - {}", getExternalServiceEndPoint());
    }

    @Override
    public boolean isSupported(String srcLang) {
       return PangeanicLanguages.isLanguageSupported(srcLang);
    }

    @Override
    public List<String> detectLang(List<String> texts, String langHint) throws LanguageDetectionException {
        try {
            HttpPost post = PangeanicTranslationUtils.createDetectlanguageRequest(getExternalServiceEndPoint(), texts, langHint, "");
            return sendDetectRequestAndParse(post);
        } catch (JSONException | IOException e) {
            throw new LanguageDetectionException(e.getMessage());
        }
    }

    /**
     * Send the request to Pangeanic and parses the response in the list of strings
     * NOTE : We do not accept results if the threshold is lower than 0.5
     * For anything not recognised or present or not acceptable , we add null values in the list
     *
     * @param post http post request with body
     * @return list of languages detected in the same sequence
     * @throws IOException
     * @throws JSONException
     * @throws LanguageDetectionException
     */
    private List<String> sendDetectRequestAndParse(HttpPost post) throws IOException, JSONException, LanguageDetectionException {
        try (CloseableHttpResponse response = detectClient.execute(post)) {
            // Pageanic BUG - sometimes language detect sends 400 Bad request with proper response and error message
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && response.getStatusLine().getStatusCode() != HttpStatus.SC_BAD_REQUEST) {
                throw new IOException("Error from Pangeanic Language Detect API: " +
                        response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
            } else {
                String json = EntityUtils.toString(response.getEntity());
                // sometimes language detect sends 200 ok status with empty response data
                if (json.isEmpty()) {
                    throw new LanguageDetectionException("Language detect returned an empty response");
                }
                JSONObject obj = new JSONObject(json);

                // if json doesn't have detected lanaguge throw a error
                if (!obj.has(PangeanicTranslationUtils.DETECTED_LANGUAGE)) {
                    throw new LanguageDetectionException("Language detect response doesn't have detected_langs tags");
                }

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
        }
    }

    private boolean hasLanguageAndScoreDetected(JSONObject object) {
        return object.has(PangeanicTranslationUtils.SOURCE_DETECTED) && object.has(PangeanicTranslationUtils.SOURCE_LANG_SCORE);
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

    public String getExternalServiceEndPoint() {
      return externalServiceEndpoint;
    }
    
    @Override
    public String getServiceId() {
      return serviceId;
    }

    @Override
    public void setServiceId(String serviceId) {
      this.serviceId=serviceId;
    }    

}
