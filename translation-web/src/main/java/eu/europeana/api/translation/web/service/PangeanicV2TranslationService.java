package eu.europeana.api.translation.web.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.translation.config.TranslationConfigProps;
import eu.europeana.api.translation.language.PangeanicLanguages;
import eu.europeana.api.translation.utils.PangeanicTranslationUtils;
import eu.europeana.api.translation.web.exception.TranslationException;

/**
 * Service to send data to translate to Pangeanic Translate API V2
 * @author Srishti Singh
 */
// TODO get api key, for now passed empty
@Service
public class PangeanicV2TranslationService implements TranslationService {

    @Autowired private PangeanicV2LangDetectService langDetectService;
    @Autowired TranslationConfigProps translConfigProps;

    protected static final Logger LOG = LogManager.getLogger(PangeanicV2TranslationService.class);
    
    protected CloseableHttpClient translateClient;

    /**
     * Creates a new client that can send translation requests to Google Cloud Translate. Note that the client needs
     * to be closed when it's not used anymore
     * @throws IOException when there is a problem retrieving the first token
     * @throws JSONException when there is a problem decoding the received token
     */
    @PostConstruct
    private void init() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(PangeanicTranslationUtils.MAX_CONNECTIONS);
        cm.setDefaultMaxPerRoute(PangeanicTranslationUtils.MAX_CONNECTIONS_PER_ROUTE);
        cm.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(3600000).build());
        //SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(3600000).build(); //We need to set socket keep alive
        translateClient = HttpClients.custom().setConnectionManager(cm).build();
        LOG.info("Pangeanic translation service is initialized with translate Endpoint - {}", translConfigProps.getPangeanicTranslateEndpoint());
    }

    /**
     * target language should be English for Pangeanic Translations
     * and validate the source language with list of supported languages
     *
     * @param srcLang source language of the data to be translated
     * @param trgLang target language in which data has to be translated
     * @return
     */
    @Override
    public boolean isSupported(String srcLang, String trgLang) {
        return PangeanicLanguages.isLanguagePairSupported(srcLang, trgLang);
    }

    @Override
    public List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage, boolean detect) throws TranslationException {
        try {
            if (detect) {
                // In this case source language is the hint. The texts passed will be sent for lang-detection first and later will translated
                return translateWithLangDetect(texts, targetLanguage, sourceLanguage);
            }
            HttpPost post = PangeanicTranslationUtils.createTranslateRequest(translConfigProps.getPangeanicTranslateEndpoint(), texts, targetLanguage, sourceLanguage, "" );
            return PangeanicTranslationUtils.getResults(texts, sendTranslateRequestAndParse(post, sourceLanguage), false);
        } catch (JSONException|IOException e) {
            throw new TranslationException(e.getMessage());
        }
    }


    /**
     * Translates the texts with no source language.
     * First a lang detect request is sent to identify the source language
     * Later translations are performed
     *
     * @param texts
     * @param targetLanguage
     * @return
     * @throws TranslationException
     */
    private List<String> translateWithLangDetect(List<String> texts, String targetLanguage, String langHint) throws TranslationException {
        try {
            List<String> detectedLanguages = langDetectService.detectLang(texts, langHint);
            // create lang-value map for translation
            Map<String, List<String>> detectedLangValueMap = PangeanicTranslationUtils.getDetectedLangValueMap(texts, detectedLanguages);
            LOG.debug("Pangeanic detect lang request with hint {} is executed. Detected languages are {} ", langHint, detectedLangValueMap.keySet());
            Map<String, String> translations = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : detectedLangValueMap.entrySet()) {
                if (PangeanicTranslationUtils.noTranslationRequired(entry.getKey())) {
                    LOG.debug("NOT translating data for lang {} for detected values {} ", entry.getKey(), entry.getValue());
                } else {
                    HttpPost translateRequest = PangeanicTranslationUtils.createTranslateRequest(translConfigProps.getPangeanicTranslateEndpoint(), entry.getValue(), targetLanguage, entry.getKey(), "");
                    translations.putAll(sendTranslateRequestAndParse(translateRequest, entry.getKey()));
                }
            }
            return PangeanicTranslationUtils.getResults(texts, translations, PangeanicTranslationUtils.nonTranslatedDataExists(detectedLanguages));
        } catch (JSONException | IOException e) {
            throw  new TranslationException(e.getMessage());
        }
    }

    private Map<String, String> sendTranslateRequestAndParse(HttpPost post, String sourceLanguage) throws IOException, JSONException, TranslationException {
        try (CloseableHttpResponse response = translateClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Error from Pangeanic Translation API: " +
                        response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
            } else {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject obj = new JSONObject(json);
                Map<String, String> results = new LinkedHashMap<>();
                // there are cases where we get an empty response
                if (!obj.has(PangeanicTranslationUtils.TRANSLATIONS)) {
                    throw new TranslationException("Pangeanic Translation API returned empty response");
                }
                JSONArray translations = obj.getJSONArray(PangeanicTranslationUtils.TRANSLATIONS);
                for (int i = 0; i < translations.length(); i++) {
                    JSONObject object = (JSONObject) translations.get(i);
                    if (hasTranslations(object)) {
                        double score = object.getDouble(PangeanicTranslationUtils.TRANSLATE_SCORE);
                        // only if score returned by the translation service is greater the threshold value, we will accept the translations
                        if (score > PangeanicLanguages.getThresholdForLanguage(sourceLanguage)) {
                            results.put(object.getString(PangeanicTranslationUtils.TRANSLATE_SOURCE), object.getString(PangeanicTranslationUtils.TRANSLATE_TARGET));
                        } else {
                            // for discarded thresholds add null as translations values
                            results.put(object.getString(PangeanicTranslationUtils.TRANSLATE_SOURCE), null);
                        }
                    }
                }
                // response should not be empty
                if (results.isEmpty()) {
                    throw new TranslationException("Translation failed for source language - " +obj.get(PangeanicTranslationUtils.SOURCE_LANG));
                }
                return  results;
            }
        }
    }

    private boolean hasTranslations(JSONObject object) {
        return object.has(PangeanicTranslationUtils.TRANSLATE_SOURCE) && object.has(PangeanicTranslationUtils.TRANSLATE_TARGET);
    }


    @Override
    public void close() {
        if (translateClient != null) {
            try {
                this.translateClient.close();
            } catch (IOException e) {
                LOG.error("Error closing connection to Pangeanic Translation API", e);
            }
        }
    }
}
