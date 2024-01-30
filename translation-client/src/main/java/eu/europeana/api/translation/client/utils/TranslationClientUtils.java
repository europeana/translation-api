package eu.europeana.api.translation.client.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import org.apache.commons.lang3.StringUtils;
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
    public static final String CONFIG = "config";
    public static final String DETECT = "detect";
    public static final String TRANSLATE = "translate";
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

    private static JsonNode getConfigNode(String json) throws TranslationApiException {
        if (!StringUtils.isEmpty(json)) {
            ObjectNode node = null;
            try {
                node = mapper.readValue(json, ObjectNode.class);
                if (node.has(CONFIG)) {
                    return node.get(CONFIG);
                }
            } catch (JsonProcessingException e) {
                throw new TranslationApiException("Error parsing the request for Translation API Info endpoint", e);

            }
        }
        return null;
    }

    public static Set<String> getDetectionLanguages(String json, Set<String> langDetectLanguages) throws TranslationApiException {
        JsonNode config = getConfigNode(json);
        if (config != null && config.has(DETECT)) {
            JsonNode detect = config.get(DETECT);
            if (detect.has(SUPPORTED)) {
                Iterator<JsonNode> iterator = detect.get(SUPPORTED).iterator();
                while (iterator.hasNext()) {
                    langDetectLanguages.add(iterator.next().asText());
                }
            }
        }
        return langDetectLanguages;
    }

    public static Set<LanguagePair> getTranslationLanguagePairs(String json, Set<LanguagePair> translationLanguages) throws TranslationApiException {
        JsonNode config = getConfigNode(json);
        if (config != null && config.has(TRANSLATE)) {
            JsonNode translate = config.get(TRANSLATE);
            if (translate.has(SUPPORTED)) {
                Iterator<JsonNode> iterator = translate.get(SUPPORTED).iterator();
                while (iterator.hasNext()) {
                    JsonNode object = iterator.next();
                    Iterator<JsonNode> sources = object.get(SOURCE).iterator();
                    Iterator<JsonNode> targets = object.get(TARGET).iterator();

                    List<String> source = new ArrayList<>();
                    List<String> target = new ArrayList<>();

                    // get all sources from the object
                    while (sources.hasNext()) {
                        source.add(sources.next().asText());
                    }
                    //  get all target from the object
                    while (targets.hasNext()) {
                        target.add(targets.next().asText());
                    }
                    // make pairs
                    source.stream().forEach(v -> target.stream().forEach(t -> translationLanguages.add(new LanguagePair(v, t))));
                    source.clear();
                    target.clear();
                }
            }
        }
        return translationLanguages;
    }
}
