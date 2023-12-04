package eu.europeana.api.translation.client.model;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.translation.client.utils.LanguageDetectionUtils;
import eu.europeana.api.translation.client.web.TranslationApiClient;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import eu.europeana.corelib.utils.ComparatorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.*;

public class TranslationMap extends LinkedHashMap<String, List<String>> {

    private static final long serialVersionUID = 7857857025275959529L;

    private static final Logger LOG = LogManager.getLogger(TranslationMap.class);

    @NotNull
    private final String sourceLanguage;

    public TranslationMap(@NotNull String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public TranslationMap(@NotNull String sourceLanguage, String fieldName, List<String> values) {
        this.sourceLanguage = sourceLanguage;
        add(fieldName, values);
    }

    /**
     * Adds the fieldname and the list of values for that field in the Translation map
     *
     * @param fieldName
     * @param values
     */
    public void add(String fieldName, List<String> values) {
        if (fieldName != null && !values.isEmpty()) {
            if (this.containsKey(fieldName)) {
                this.get(fieldName).addAll(values);
            } else {
                this.put(fieldName, values);
            }
        }
    }

    /**
     * Translates the field value map using the translation service provided in the target Language
     * We know already with the translation workflow, there is only one source language (chosen language)
     * in which all the data for the fields is gathered
     *
     * @param translationApiClient Translation api client for translations
     * @param targetLanguage       language in which values are to be translated
     * @return translation map with target language and translations
     * @throws EuropeanaApiException thrown from the client
     */
    public TranslationMap translate(TranslationApiClient translationApiClient, String targetLanguage) throws EuropeanaApiException {
        // save the field name and size per field (number of values associated with it)
        // to retain the order using LinkedHashmap and get all the texts for translations
        Map<String, Integer> textsPerField = new LinkedHashMap<>();
        List<String> textsToTranslate = new ArrayList<>();
        addTextsAndPerFieldCount(textsToTranslate, textsPerField);

        // send request for translation
        LOG.debug("Sending translate request with target language - {} and source language - {}", targetLanguage, this.sourceLanguage);
        TranslationResponse response = translationApiClient.translate(createTranslationRequest(textsToTranslate, targetLanguage));
        List<String> translations = response.getTranslations();
        LOG.debug("Translation API service used for translations - {} ", response.getService());

        // fail safe check
        if (translations.size() != textsToTranslate.size()) {
            throw new IllegalStateException("Expected " + textsToTranslate.size() + " lines of translated text, but received " + translations.size());
        }

        // create the target language - translated map from the translations received from the service
        TranslationMap translatedMap = new TranslationMap(targetLanguage);

        // if only nulls are returned no need to do anything further
        if (LanguageDetectionUtils.onlyNulls(translations)) {
            return translatedMap;
        }

        int fromIndex = 0;
        for (Map.Entry<String, Integer> entry : textsPerField.entrySet()) {
            int toIndex = fromIndex + entry.getValue();

            // get the translation values for the field. We do not want to modify translation list hence deep copy
            List<String> values = new ArrayList<>();
            values.addAll(translations.subList(fromIndex, toIndex));
            translatedMap.add(entry.getKey(), getTranslationsToAdd(values));
            fromIndex += entry.getValue();
        }
        return translatedMap;
    }

    private void  addTextsAndPerFieldCount(List<String> textsToTranslate, Map<String, Integer> textsPerField) {
        for (Map.Entry<String, List<String>> entry : this.entrySet()) {
            textsToTranslate.addAll(entry.getValue());
            textsPerField.put(entry.getKey(), entry.getValue().size());
        }
    }

    private TranslationRequest createTranslationRequest(List<String> textsToTranslate, String targetLanguage) {
        TranslationRequest translationRequest = new TranslationRequest();
        translationRequest.setText(textsToTranslate);
        translationRequest.setSource(this.sourceLanguage);
        translationRequest.setTarget(targetLanguage);
        //translationRequest.setService("GOOGLE");
        return translationRequest;
    }

    /**
     * Returns translations for the specific field after removing null and duplicates
     *
     * @param translationsForField translations for that field
     * @return
     */
    private List<String> getTranslationsToAdd(List<String> translationsForField) {
        // remove null values for discarded translations due to lower thresholds or other reasons
        translationsForField.removeIf(Objects::isNull);
        ComparatorUtils.removeDuplicates(translationsForField);
        return translationsForField;
    }

    @NotNull
    public String getSourceLanguage() {
        return sourceLanguage;
    }
}
