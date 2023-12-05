package eu.europeana.api.translation.client.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.translation.client.TranslationApiClient;
import eu.europeana.api.translation.definitions.language.Language;
import eu.europeana.api.translation.client.model.TranslationMap;
import eu.europeana.api.translation.client.utils.LanguageDetectionUtils;
import eu.europeana.corelib.definitions.edm.entity.ContextualClass;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
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

    private final MetadataChosenLanguageService metadataChosenLanguageService;

    public MetadataTranslationService(TranslationApiClient translationApiClient, MetadataChosenLanguageService metadataChosenLanguageService) {
        super(translationApiClient);
        this.metadataChosenLanguageService = metadataChosenLanguageService;
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

    public FullBean translationWorkflow(FullBean bean, String targetLanguage) throws EuropeanaApiException {
        long start = System.currentTimeMillis();
        List<Proxy> proxies = new ArrayList<>(bean.getProxies()); // make sure we clone first so we can edit the list to our needs.

        String chosenLanguage = metadataChosenLanguageService.getMostRepresentativeLanguage(bean, targetLanguage);

        // if there is no chosen language stop the translation workflow OR
        // If the chosen language matches pivot language (ie. English) then do nothing (stop the workflow)
        if (chosenLanguage == null || StringUtils.equals(chosenLanguage, Language.PIVOT)) {
            LOG.debug("Stop the translation workflow for record {}", bean.getAbout());
            return bean;
        }

        LOG.debug("Most representative language chosen for translations is  {}", chosenLanguage);

        TranslationMap textToTranslate = new TranslationMap(chosenLanguage);

        // To store the fields if they have "en" values across any proxy
        Set<String> otherProxyFieldsWithEnglishValues = new HashSet<>();

        for (Proxy proxy : proxies) {
            ReflectionUtils.doWithFields(proxy.getClass(), field -> getProxyValuesToTranslateForField(proxy, field, chosenLanguage, bean, textToTranslate, otherProxyFieldsWithEnglishValues), proxyFieldFilter);
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
        TranslationMap translations = textToTranslate.translate(getTranslationApiClient(), targetLanguage);
        if (translations.isEmpty()) {
            LOG.debug("Empty or null translation returned by the Translation API Client");
            return bean;
        }

        // add all the translated data to Europeana proxy
        Proxy europeanaProxy = getEuropeanaProxy(bean.getProxies(), bean.getAbout());
        updateProxy(europeanaProxy, translations);

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
        HashMap<String, List<String>> origFieldData = (HashMap<String, List<String>>) getValueOfTheField(proxy, false).apply(field.getName());
        getValueFromLanguageMap(SerializationUtils.clone(origFieldData), field, sourceLang, bean, map, otherProxyFieldsWithEnglishValues);

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
    private void getValueFromLanguageMap(HashMap<String, List<String>> origFieldData, Field field, String sourceLang, FullBean bean, TranslationMap map,
                                         Set<String> otherProxyFieldsWithEnglishValues) {

        // Get the value only if there is NO "en" language tag already present for the field in any proxy and there is value present for the sourceLang
        if (origFieldData != null && !origFieldData.isEmpty()  && !origFieldData.containsKey(Language.PIVOT) && origFieldData.containsKey(sourceLang)) {
            List<String> valuesToTranslateForField = getValuesToTranslate(origFieldData, sourceLang, bean);
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
    private List<String> getValuesToTranslate(HashMap<String, List<String>> origFieldData, String sourceLang, FullBean bean) {
        List<String> valuesToTranslate = new ArrayList<>();
        for (String value : origFieldData.get(sourceLang)) {
            // if the value is a URI get the contextual entity pref label in source lang.
            // Also, ignore the other uri values whose entity doesn't exist
            if (EuropeanaUriUtils.isUri(value)) {
                ContextualClass entity = entityExistsWithUrl(bean, value);
                if (entity != null && entity.getPrefLabel() != null && entity.getPrefLabel().containsKey(sourceLang)) {
                    LOG.debug("Entity {} has preflabel in chosen language {} for translation  ", value, sourceLang);
                    valuesToTranslate.addAll(entity.getPrefLabel().get(sourceLang));
                }
            } else {
                valuesToTranslate.add(value); // add non uri values
            }
        }
        // remove duplicate values and also filter values with atleast 1 unicode or number
        return LanguageDetectionUtils.filterValuesWithAtleastOneUnicodeOrNumber(ComparatorUtils.removeDuplicates(valuesToTranslate));
    }



    /**
     * Updates the proxy object field values by adding the new map values
     * @param proxy
     * @param translatedMap
     */
    private void updateProxy( Proxy proxy, TranslationMap translatedMap) {
        translatedMap.entrySet().stream().forEach(value -> {
            Map<String, List<String>> existingMap = getValueOfTheField(proxy, true).apply(value.getKey());
            // get the "en" values or default to empty list
            List<String> enValues = existingMap.getOrDefault(translatedMap.getSourceLanguage(), new ArrayList<>());
            enValues.addAll(value.getValue());
            // update the "en" map
            existingMap.compute(translatedMap.getSourceLanguage(), (key, val)-> enValues);
        });
    }

}
