package eu.europeana.api.translation.record.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.translation.client.TranslationApiClient;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.definitions.language.Language;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import eu.europeana.api.translation.record.model.TranslationMap;
import eu.europeana.api.translation.record.utils.LanguageDetectionUtils;
import eu.europeana.corelib.definitions.edm.beans.BriefBean;
import eu.europeana.corelib.definitions.edm.entity.ContextualClass;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import eu.europeana.corelib.edm.utils.EdmUtils;
import eu.europeana.corelib.utils.ComparatorUtils;
import eu.europeana.corelib.utils.EuropeanaUriUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Service for Metadata translation workflow
 *
 * @author srishti singh
 *
 */
public class MetadataTranslationService extends BaseService {

    private static final Logger LOG = LogManager.getLogger(MetadataTranslationService.class);
    private static final String FIELD_SEPARATOR = ".";
    private static final String FIELD_SEPARATOR_REGEX = "\\.";

    private final MetadataChosenLanguageService metadataChosenLanguageService;

    public MetadataTranslationService(TranslationApiClient translationApiClient, MetadataChosenLanguageService metadataChosenLanguageService) {
        super(translationApiClient);
        this.metadataChosenLanguageService = metadataChosenLanguageService;
    }

    public List<BriefBean> searchResultsTranslations(List<BriefBean> beans, String targetLanguage) throws TranslationApiException {
        long start = System.currentTimeMillis();

        String chosenLanguage = metadataChosenLanguageService.getMostRepresentativeLanguageForSearch(beans, targetLanguage);

        // if there is no chosen language stop the translation workflow OR
        // If the chosen language matches target language then do nothing (stop the workflow)
        // keep in mind during Ingestion we always translate the record to english. So target language is always en
        if (chosenLanguage == null || StringUtils.equals(chosenLanguage, targetLanguage)) {
            LOG.debug("Stop the translation workflow for search results ..." );
            return beans;
        }

        LOG.debug("Most representative language chosen for translations is  {}", chosenLanguage);

        TranslationMap textToTranslate = new TranslationMap(chosenLanguage);

        int index = 0;
        for (BriefBean bean : beans) {
            LOG.debug("Check search result {}...", index);
            int finalIndex = index;
            ReflectionUtils.doWithFields(bean.getClass(), field ->  getSearchValueToTranslateForField(bean, field, chosenLanguage, textToTranslate, finalIndex), BaseService.searchFieldFilter);
            index++;
        }

        // if no translation gathered return
        if (textToTranslate.isEmpty()) {
            LOG.debug("No values gathered for translations. Stopping the translation workflow for search results");
            return beans;
        }

        LOG.debug("Text to translate - {}", textToTranslate);

        // get the translation in the target language
        TranslationMap translations = translate(textToTranslate, targetLanguage);
        if (translations.isEmpty()) {
            LOG.debug("Empty or null translation returned by the Translation API Client");
            return beans;
        }

        // add all the translated data to respective bean result
        translations.entrySet().stream().forEach(value -> {
            String[] parts = value.getKey().split(FIELD_SEPARATOR_REGEX);
            int i = Integer.parseInt(parts[0]);
            String fieldName = parts[1];
            LOG.trace("Updating {} index result for field..", i, fieldName);

            addTranslationToObject(beans.get(i), fieldName, value.getValue(), translations.getSourceLanguage());
        });

        LOG.debug("Translating search results took {} ms", (System.currentTimeMillis() - start));
        return beans;
    }



    /**
     * Iterate over all the proxies fields and returns a translated updated bean in the target lanaguge
     *
     * Translation WorkFlow :
     *   1. Choose the language to translate from by finding the most representative language in the metadata
     *        Most representative language is the one that has the most values for the whitelisted properties from all Proxies
     *        Ignore values with language code “zxx” and unsupported languages (ie. call the isSupported method)
     *        If there is a tie, choose based on the precedence list3 of the two (or more)
     *
     *   2. If the chosen language is English then do nothing (stop the workflow)
     *
     *   3. Gather all language qualified values matching the chosen language per whitelisted property from all
     *      Proxies including the Europeana Proxy. If there is already a English value for a property then skip
     *      this property (do not select any value from it)
     *
     *     If there is already a value matching the pivot language (ie. English) for a property then skip this property (do not select any value from it)
     *     Check if the value contains at least 1 unicode letter or number (otherwise ignore for translation)
     *     For contextual entities, consider only the value from the skos:prefLabel
     *
     *   4. Check if there is anything to translate, if not do nothing (stop workflow)
     *
     *   5. Translate all values (of the chosen language) to English (ie. call translate method)
     *      Eliminate any duplicate values for each property
     *
     *   6. Add all target language translations to the respective property in the Europeana Proxy
     *
     *
     */

    public FullBean proxyTranslation(FullBean bean, String targetLanguage) throws TranslationApiException {
        long start = System.currentTimeMillis();
        List<Proxy> proxies = new ArrayList<>(bean.getProxies()); // make sure we clone first so we can edit the list to our needs.

        String chosenLanguage = metadataChosenLanguageService.getMostRepresentativeLanguageForProxy(bean, targetLanguage);

        // if there is no chosen language stop the translation workflow OR
        // If the chosen language matches target language then do nothing (stop the workflow)
        // keep in mind during Ingestion we always translate the record to english. So target language is always en
        if (chosenLanguage == null || StringUtils.equals(chosenLanguage, targetLanguage)) {
            LOG.debug("Stop the translation workflow for record {}", bean.getAbout());
            return bean;
        }

        LOG.debug("Most representative language chosen for translations is  {}", chosenLanguage);

        TranslationMap textToTranslate = new TranslationMap(chosenLanguage);

        // To store the fields if they have "en" values across any proxy
        Set<String> otherProxyFieldsWithEnglishValues = new HashSet<>();

        for (Proxy proxy : proxies) {
            ReflectionUtils.doWithFields(proxy.getClass(), field -> getProxyValuesToTranslateForField(proxy, field, chosenLanguage, bean, textToTranslate, otherProxyFieldsWithEnglishValues), BaseService.proxyFieldFilter);
        }

        // remove the fields whose "en" values are present in other proxies
        otherProxyFieldsWithEnglishValues.stream().forEach(field -> {
            if (textToTranslate.containsKey(field)) {
                textToTranslate.remove(field);
            }
        });

        // if no translation gathered return
        if (textToTranslate.isEmpty()) {
            LOG.debug("No values gathered for translations. Stopping the translation workflow for record {}", bean.getAbout());
            return bean;
        }

        // get the translation in the target language
        TranslationMap translations = translate(textToTranslate, targetLanguage);
        if (translations.isEmpty()) {
            LOG.debug("Empty or null translation returned by the Translation API Client");
            return bean;
        }

        // add all the translated data to Europeana proxy
        Proxy europeanaProxy = BaseService.getEuropeanaProxy(bean.getProxies(), bean.getAbout());
        translations.entrySet().stream().forEach(value -> addTranslationToObject(europeanaProxy, value.getKey(), value.getValue(), translations.getSourceLanguage()));

        LOG.debug("Translating record {} took {} ms", bean.getAbout(), (System.currentTimeMillis() - start));
        return bean;
    }

    /**
     * Returns Proxy value to translate for the given field in the map
     * @param proxy
     * @param field
     * @param sourceLang
     * @param bean
     * @return
     */
    private void getProxyValuesToTranslateForField(Proxy proxy, Field field, String sourceLang, FullBean bean, TranslationMap map, Set<String> otherProxyFieldsWithEnglishValues) {
        HashMap<String, List<String>> origFieldData = (HashMap<String, List<String>>) BaseService.getValueOfTheField(proxy, false).apply(field.getName());
        getValueFromLanguageMap(SerializationUtils.clone(origFieldData), field, sourceLang, bean, map, otherProxyFieldsWithEnglishValues);

    }

    /**
     * Returns Serach result values to translate for the given field in the map
     *
     *  Map keys we get from Solr have compound names with a dot (e.g. {proxy_dc_title.ro=[Happy-end]})
     *  The EdmUtils.cloneMap functionality makes sure that is transformed into something we can use (e.g. def)
     * @param bean
     * @param field
     * @param sourceLang
     * @param bean
     * @return
     */
    private void getSearchValueToTranslateForField(BriefBean bean, Field field, String sourceLang, TranslationMap map, int index) {
        HashMap<String, List<String>> origFieldData = (HashMap<String, List<String>>) BaseService.getValueOfTheField(bean, false).apply(field.getName());
        Map<String, List<String>> fieldData = EdmUtils.cloneMap(origFieldData);

        // Get the value only if there is NO "en" language tag already present for the field and there is value present for the sourceLang
        if (fieldData != null && !fieldData.isEmpty()  && !fieldData.containsKey(Language.PIVOT) && fieldData.containsKey(sourceLang)) {
            List<String> valuesToTranslateForField = getValuesToTranslate((HashMap<String, List<String>>) fieldData, sourceLang, null, true);
            if (!valuesToTranslateForField.isEmpty()) {
                map.add(index + FIELD_SEPARATOR + field.getName(), valuesToTranslateForField);
            }
        }
    }

    /**
     * Returns the language qualified values matching the chosen language
     * If there is already a English value for a property then skip this property (do not select any value from it)
     * For contextual entities, consider only the value from the skos:prefLabel
     *
     * @param origFieldData field lang value map
     * @param field         field name (from the whitelisted fields)
     * @param sourceLang    the language chosen for translation
     * @param bean          record
     * @return
     */
    private void getValueFromLanguageMap(HashMap<String, List<String>> origFieldData, Field field, String sourceLang, FullBean bean, TranslationMap map, Set<String> otherProxyFieldsWithEnglishValues) {
        // Get the value only if there is NO "en" language tag already present for the field in any proxy and there is value present for the sourceLang
        if (origFieldData != null && !origFieldData.isEmpty()  && !origFieldData.containsKey(Language.PIVOT) && origFieldData.containsKey(sourceLang)) {
            List<String> valuesToTranslateForField = getValuesToTranslate(origFieldData, sourceLang, bean, false);
            if (!valuesToTranslateForField.isEmpty()) {
                map.add(field.getName(),valuesToTranslateForField);
            }
        }
        // if contains english add it in the list
        if (origFieldData != null && !origFieldData.isEmpty()  && origFieldData.containsKey(Language.PIVOT)) {
            otherProxyFieldsWithEnglishValues.add(field.getName());
        }
    }

    /**
     * Returns list of values to be translated.
     * Looks for Contextual entities, if found fetches the prefLabel of the entity in the source language
     *
     * @param origFieldData field lang value map
     * @param sourceLang    the language chosen for translation
     * @param bean          record
     * @return
     */
    private List<String> getValuesToTranslate(HashMap<String, List<String>> origFieldData, String sourceLang, FullBean bean, boolean onlyLiterals) {
        List<String> valuesToTranslate = new ArrayList<>();
        // for search translations we only need literal values
        if (onlyLiterals) {
            valuesToTranslate = LanguageDetectionUtils.filterOutUris(origFieldData.get(sourceLang));
        } else {
            for (String value : origFieldData.get(sourceLang)) {
                // if the value is a URI get the contextual entity pref label in source lang.
                // Also, ignore the other uri values whose entity doesn't exist
                if (EuropeanaUriUtils.isUri(value)) {
                    ContextualClass entity = BaseService.entityExistsWithUrl(bean, value);
                    if (entity != null && entity.getPrefLabel() != null && entity.getPrefLabel().containsKey(sourceLang)) {
                        LOG.debug("Entity {} has preflabel in chosen language {} for translation  ", value, sourceLang);
                        valuesToTranslate.addAll(entity.getPrefLabel().get(sourceLang));
                    }
                } else {
                    valuesToTranslate.add(value); // add non uri values
                }
            }
        }
        // remove duplicate values and also filter values with atleast 1 unicode or number
        return LanguageDetectionUtils.filterValuesWithAtleastOneUnicodeOrNumber(ComparatorUtils.removeDuplicates(valuesToTranslate));
    }


    /**
     * Updates the object with translations results
     * @param object object to be updated
     * @param fieldName field to be updated in the object
     * @param translatedValues list of translated values to be added
     * @param targetLanguage language for the translated values
     */
    private void addTranslationToObject(Object object, String fieldName, List<String> translatedValues, String targetLanguage) {
        Map<String, List<String>> existingMap = BaseService.getValueOfTheField(object, true).apply(fieldName);
        List<String> targetLangValues = existingMap.getOrDefault(targetLanguage, new ArrayList<>());
        targetLangValues.addAll(translatedValues);
        existingMap.compute(targetLanguage, (key, val)-> targetLangValues);
    }

    /**
     * Translates the field value map using the translation service provided in the target Language
     * We know already with the translation workflow, there is only one source language (chosen language)
     * in which all the data for the fields is gathered
     *
     * @param targetLanguage       language in which values are to be translated
     * @return translation map with target language and translations
     * @throws EuropeanaApiException thrown from the client
     */
    private TranslationMap translate(TranslationMap map, String targetLanguage) throws TranslationApiException {
        // save the field name and size per field (number of values associated with it)
        // to retain the order using LinkedHashmap and get all the texts for translations
        Map<String, Integer> textsPerField = new LinkedHashMap<>();
        List<String> textsToTranslate = new ArrayList<>();
        addTextsAndPerFieldCount(map, textsToTranslate, textsPerField);

        // send request for translation
        LOG.debug("Sending translate request with target language - {} and source language - {}", targetLanguage, map.getSourceLanguage());
        TranslationResponse response = getTranslationApiClient().translate(createTranslationRequest(textsToTranslate, targetLanguage, map.getSourceLanguage()));
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

    private void  addTextsAndPerFieldCount(TranslationMap map, List<String> textsToTranslate, Map<String, Integer> textsPerField) {
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            textsToTranslate.addAll(entry.getValue());
            textsPerField.put(entry.getKey(), entry.getValue().size());
        }
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
}
