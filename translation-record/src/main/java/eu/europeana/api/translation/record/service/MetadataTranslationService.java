package eu.europeana.api.translation.record.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.translation.definitions.language.Language;
import eu.europeana.api.translation.record.model.TranslationMap;
import eu.europeana.api.translation.record.utils.LanguageDetectionUtils;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;
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
import java.util.stream.Collectors;

/**
 * Service for Metadata translation workflow
 *
 * @author srishti singh
 *
 */
public class MetadataTranslationService extends BaseService {

    private static final Logger LOG = LogManager.getLogger(MetadataTranslationService.class);

    private final TranslationService translationService;

    public MetadataTranslationService(TranslationService translationService) {
        this.translationService = translationService;
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

    public FullBean translationWorkflow(FullBean bean, String targetLanguage) throws TranslationException, EuropeanaApiException {
        long start = System.currentTimeMillis();
        List<Proxy> proxies = new ArrayList<>(bean.getProxies()); // make sure we clone first so we can edit the list to our needs.

        String chosenLanguage = getMostRepresentativeLanguage(bean, targetLanguage);

        // if there is no chosen language stop the translation workflow OR
        // If the chosen language matches pivot language (ie. English) then do nothing (stop the workflow)
        if (chosenLanguage == null || StringUtils.equals(chosenLanguage, Language.ENGLISH)) {
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
        TranslationMap translations = textToTranslate.translate(translationService, targetLanguage);

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
        if (origFieldData != null && !origFieldData.isEmpty()  && !origFieldData.containsKey(Language.ENGLISH) && origFieldData.containsKey(sourceLang)) {
            List<String> valuesToTranslateForField = getValuesToTranslate(origFieldData, sourceLang, bean);
            if (!valuesToTranslateForField.isEmpty()) {
                map.add(field.getName(),valuesToTranslateForField);
            }
        }
        // if contains english add it in the list
        if (origFieldData != null && !origFieldData.isEmpty()  && origFieldData.containsKey(Language.ENGLISH)) {
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
     * Will fetch the most representative langiuage from all the proxies,  aka - chosen language
     * @param bean
     */
    public String getMostRepresentativeLanguage(FullBean bean, String targetLanguage) {
        Map<String, Integer> langCountMap = new HashMap<>();
        List<? extends Proxy> proxies = bean.getProxies();

        for (Proxy proxy : proxies) {
            ReflectionUtils.doWithFields(proxy.getClass(), field -> getLanguageAndCount(proxy, field, langCountMap, targetLanguage), proxyFieldFilter);
        }

        // if there is no language available for translation workflow, do nothing
        if (langCountMap.isEmpty()) {
            LOG.error("Most representative languages NOT present for record {}. " +
                    "Languages present are either zxx or def or not-supported by the translation engine", bean.getAbout());
            return null;
        }

        //reverse map - as values might not be unique so using grouping method
        Map<Integer, List<String>> reverseMap =
                langCountMap.entrySet()
                        .stream()
                        .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        List<String> languagesWithMostvalues = reverseMap.get(Collections.max(reverseMap.keySet()));

        // if there is a tie between more than one language, choose based on the precedance list
        if (languagesWithMostvalues.size() > 1) {
            Optional<String> langWithHigherPrecedance =  PRECENDANCE_LIST.stream().filter(languagesWithMostvalues :: contains).findFirst();
            if (langWithHigherPrecedance.isPresent()) {
                return langWithHigherPrecedance.get();
            } else {
                LOG.warn("Language not found in the precedence list. Hence, will return the first language out of - {} ", languagesWithMostvalues);
            }
        }
        // will only have one value here, hence by default or any else case return the first language.
        // Also if we had multiple values and those languages were not present in the precedence list (this is an exceptional case, should not happen)
        // but in those cases as well just any random value is acceptable( we will return the first language)
        return languagesWithMostvalues.get(0);
    }


    private void getLanguageAndCount(Proxy proxy, Field field, Map<String, Integer> langCountMap, String targetLang) {
        Map<String, List<String>> langValueMap = getValueOfTheField(proxy, false).apply(field.getName());
        if (!langValueMap.isEmpty()) {
            for (Map.Entry<String, List<String>> langValue : langValueMap.entrySet()) {
                String key = langValue.getKey();
                if (languageToBeChosen(key, targetLang)) {
                    Integer value = langValue.getValue().size();
                    if (langCountMap.containsKey(key)) {
                        value += langCountMap.get(key);
                    }
                    langCountMap.put(key, value);
                }
            }
        }
    }

    /**
     * Identifies if Language should be selected as most representative
     * Ignores the values with language code “zxx” or "def" and unsupported languages (ie. call the isSupported method)
     * <p>
     * NOTE : We check if the translation is supported for the language pair.
     *
     * @param lang value
     * @return true if language should be chosen
     */

    private boolean languageToBeChosen(String lang, String targetLanguage) {
        return !(StringUtils.equals(lang, Language.NO_LINGUISTIC_CONTENT) || StringUtils.equals(lang, Language.DEF))
                && translationService.isSupported(lang, targetLanguage);
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
