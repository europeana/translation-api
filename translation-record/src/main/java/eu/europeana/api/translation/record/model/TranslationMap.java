package eu.europeana.api.translation.record.model;

import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.corelib.utils.ComparatorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public TranslationMap translate(TranslationService translationService, String targetLanguage) throws TranslationException {
        // save the field name and size per field (number of values associated with it)
        Map<String, Integer> textsPerField = new LinkedHashMap<>();
        List<TranslationObj> translationObjs = new ArrayList<>();

        // create Translation objects
        for (Map.Entry<String, List<String>> entry : this.entrySet()) {
            textsPerField.put(entry.getKey(), entry.getValue().size());
            entry.getValue().stream().forEach(value -> translationObjs.add(new TranslationObj(value, this.sourceLanguage, targetLanguage)));
        }

        // send request for translation
        LOG.debug("Sending translate request with target language - {} and source language - {}", targetLanguage, this.sourceLanguage);
        translationService.translate(translationObjs);

        // create the target language - translated map from the translations received from the service
        TranslationMap translatedMap = new TranslationMap(targetLanguage);


        int fromIndex = 0;
        for (Map.Entry<String, Integer> entry : textsPerField.entrySet()) {
            List<String> translatedValues = new ArrayList<>();
            for (int i = fromIndex; i < entry.getValue() + fromIndex; i++) {
                translatedValues.add(translationObjs.get(i).getTranslation());
            }
            // remove duplicate translated values if added
            translatedMap.add(entry.getKey(), ComparatorUtils.removeDuplicates(translatedValues));
            fromIndex = entry.getValue();
        }
        return translatedMap;
    }


    @NotNull
    public String getSourceLanguage() {
        return sourceLanguage;
    }
}