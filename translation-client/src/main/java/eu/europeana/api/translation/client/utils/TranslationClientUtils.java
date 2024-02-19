package eu.europeana.api.translation.client.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.LangDetectRequest;
import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.*;
import java.util.function.Function;

public class TranslationClientUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String TRANSLATE_URL = "translate";
    public static final String LANG_DETECT_URL = "detect";
    public static final String INFO_ENDPOINT_URL = "actuator/info";

    // info endpoint constants
    public static final String CONFIG_DETECT = "/config/detect";
    public static final String CONFIG_TRANSLATE = "/config/translate";
    public static final String SUPPORTED = "supported";
    public static final String SOURCE = "source";
    public static final String TARGET = "target";

    private TranslationClientUtils() {

    }

    /**
     * Builds the url path based on the path string passed
     *
     * @param path
     * @return
     */
    public static Function<UriBuilder, URI> buildUrl(String path) {
        return uriBuilder -> {
            UriBuilder builder =
                    uriBuilder
                            .path("/" + path);
            return builder.build();
        };
    }

    /**
     * Creates LangDetectRequest from LanguageDetectionObj
     * @param languageDetectionObjs
     * @return
     */
    public static LangDetectRequest createLangDetectRequest(List<LanguageDetectionObj> languageDetectionObjs) {
        LangDetectRequest langDetectRequest = new LangDetectRequest();
        // hint is optional
        String hint = languageDetectionObjs.get(0).getHint();
        if (StringUtils.isNotEmpty(hint)) {
            langDetectRequest.setLang(hint);
        }
        List<String> text = new ArrayList<>(languageDetectionObjs.size());
        for (LanguageDetectionObj object : languageDetectionObjs) {
            text.add(object.getText());
        }
        langDetectRequest.setText(text);
        return langDetectRequest;
    }

    /**
     * Creates TranslationRequest from TranslationObj
     * @param translationStrings
     * @return
     */
    public static TranslationRequest createTranslationRequest(List<TranslationObj> translationStrings) {
        TranslationRequest translationRequest = new TranslationRequest();
        translationRequest.setSource(translationStrings.get(0).getSourceLang());
        translationRequest.setTarget(translationStrings.get(0).getTargetLang());

        List<String> text = new ArrayList<>(translationStrings.size());
        for (TranslationObj object : translationStrings) {
            text.add(object.getText());
        }
        translationRequest.setText(text);
        return translationRequest;
    }

    private static JsonNode getConfigNode(String json, String nodeToFetch) throws TranslationApiException {
        if (!StringUtils.isEmpty(json)) {
            try {
                return mapper.readTree(json).at(nodeToFetch);
            } catch (JsonProcessingException e) {
                throw new TranslationApiException("Error fetching the node - " + nodeToFetch + "from the Translation API Info endpoint", HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
            }
        }
        return null;
    }

    public static Set<String> getDetectionLanguages(String json, Set<String> langDetectLanguages) throws TranslationApiException {
        JsonNode detect = getConfigNode(json, CONFIG_DETECT);
        if (detect.has(SUPPORTED)) {
            Iterator<JsonNode> iterator = detect.get(SUPPORTED).iterator();
            while (iterator.hasNext()) {
                langDetectLanguages.add(iterator.next().asText());
            }
        }
        return langDetectLanguages;
    }

    public static Set<LanguagePair> getTranslationLanguagePairs(String json, Set<LanguagePair> translationLanguages) throws TranslationApiException {
        JsonNode translate = getConfigNode(json, CONFIG_TRANSLATE);
        if (translate.has(SUPPORTED)) {
            Iterator<JsonNode> iterator = translate.get(SUPPORTED).iterator();
            while (iterator.hasNext()) {
                JsonNode object = iterator.next();
                List<String> source = getIteratorValue(object.get(SOURCE).iterator());
                List<String> target = getIteratorValue(object.get(TARGET).iterator());
                // make pairs
                source.stream().forEach(v -> target.stream().forEach(t -> translationLanguages.add(new LanguagePair(v, t))));
            }
        }

        return translationLanguages;
    }

    private static List<String> getIteratorValue(Iterator<JsonNode> jsonNodeIterator) {
        List<String> values = new ArrayList<>();
        while (jsonNodeIterator.hasNext()) {
            values.add(jsonNodeIterator.next().asText());
        }
        return values;
    }
}
