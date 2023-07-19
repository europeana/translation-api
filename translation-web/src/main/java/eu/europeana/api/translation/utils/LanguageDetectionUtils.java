package eu.europeana.api.translation.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.europeana.api.translation.language.Language;
import eu.europeana.api.translation.language.LanguageValueFieldMap;

@Deprecated
/**
 * @deprecated record translation not part of the translation API
 * @author GordeaS
 *
 */
public class LanguageDetectionUtils {

    private static final Logger LOG = LogManager.getLogger(LanguageDetectionUtils.class);

    private static final String PATTERN = "\\p{L}|[0-9]";
    private static final Pattern unicodeNumberPattern = Pattern.compile(PATTERN);

    private LanguageDetectionUtils() {

    }

    /**
     * Returns the default language list of the edm:languages
     * NOTE : For region locales values, if present in edm:languages
     * the first two ISO letters will be picked up.
     * <p>
     * Only returns the supported official languages,See: {@link Language}
     * Default translation and filtering for non-official language
     * is not supported
     *
     * @param bean the fullbean to inspect
     * @return the default language as specified in Europeana Aggregation edmLanguage field (if the language found there
     * is one of the EU languages we support in this application for translation)
     */
//    public static List<Language> getEdmLanguage(FullBean bean) {
//        List<Language> lang = new ArrayList<>();
//        Map<String, List<String>> edmLanguage = bean.getEuropeanaAggregation().getEdmLanguage();
//        for (Map.Entry<String, List<String>> entry : edmLanguage.entrySet()) {
//            for (String languageAbbreviation : entry.getValue()) {
//                if (Language.isSupported(languageAbbreviation)) {
//                    lang.add(Language.getLanguage(languageAbbreviation));
//                } else {
//                    LOG.warn("edm:language '{}' is not supported", languageAbbreviation);
//                }
//            }
//        }
//        if (!lang.isEmpty()) {
//            LOG.debug("EDM language - {} fetched for record - {} ", lang, bean.getAbout());
//        }
//        return lang;
//    }


    /**
     * Method to get values of non-language tagged prefLabel (only if no other language tagged value doesn't exists)
     * @param entity entity object
     * @return
     */
//    public static List<String> getPrefLabelofEntity(ContextualClass entity, String recordId) {
//        List<String> prefLabels = new ArrayList<>();
//        if (entity != null) {
//            if (entity.getPrefLabel() != null) {
//                Map<String, List<String>> map = entity.getPrefLabel();
//                if (!map.isEmpty() && !map.keySet().isEmpty()) {
//                    // if preflabel is present in other languages than "def" then do nothing
//                    if (!map.isEmpty() && !map.keySet().isEmpty() && mapHasOtherLanguagesThanDef(map.keySet())) {
//                        LOG.debug("Entity {} already has language tagged values. PrefLabels NOT added...", entity.getAbout());
//                    } else { // pick the def value
//                        LOG.debug("Entity {} has only non-language tagged values. Adding the prefLabels...", entity.getAbout());
//                        prefLabels.addAll(map.get(Language.DEF));
//                    }
//                }
//            } else {
//                LOG.error("prefLabels NOT available for entity {} in record {} .", entity.getAbout(), recordId);
//            }
//        }
//        return prefLabels;
//    }

    /**
     * This methods adds the texts to be sent for detection in a list.
     * Additionally also saves the texts sent per field for detection
     *
     * @param textsForDetection List to store texts to be sent for language detection
     * @param textsPerField to add the text size sent for detection per field
     * @param langValueFieldMapForDetection lang-value "def" map for the whitelisted field
     */
    public static void getTextsForDetectionRequest(List<String> textsForDetection,
                                                   Map<String, Integer> textsPerField, List<LanguageValueFieldMap> langValueFieldMapForDetection ) {
        for (LanguageValueFieldMap languageValueFieldMap : langValueFieldMapForDetection) {
            for (Map.Entry<String, List<String>> def : languageValueFieldMap.entrySet()) {
                textsForDetection.addAll(def.getValue());
                textsPerField.put(languageValueFieldMap.getFieldName(), def.getValue().size());
            }
        }
    }

    /**
     * Assigns the correct language to the values for the fields
     * @param textsPerField number of texts present per field list
     * @param detectedLanguages languages detected by the Engine
     * @param textsForDetection texts sent for lang-detect requests
     * @return
     */
    public static List<LanguageValueFieldMap> getLangDetectedFieldValueMap(Map<String, Integer> textsPerField, List<String> detectedLanguages, List<String> textsForDetection) {
        int counter =0;
        List<LanguageValueFieldMap> correctLangMap = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : textsPerField.entrySet()) { // field loop
            // new value map for field
            Map<String, List<String>> newValueMap = new HashMap<>();

            for(int i=0; i< entry.getValue(); i++) {
                String newLang = detectedLanguages.get(counter);
                // if the service did not return any language for the text then source language should be kept intact
                // which is "def" in these cases
                newLang = newLang == null ? Language.DEF : newLang;
                if (newValueMap.containsKey(newLang)) {
                    newValueMap.get(newLang).add(textsForDetection.get(counter));
                } else {
                    List<String> values = new ArrayList<>();
                    values.add(textsForDetection.get(counter));
                    newValueMap.put(newLang, values);
                }
                counter++;
            }
            // add the new map for the field
            correctLangMap.add(new LanguageValueFieldMap(entry.getKey(), newValueMap));
        }
        return correctLangMap;
    }

    /**
     * Returns the def values of the field (removing the values which are already present in the lang-tagged)
     *
     * @param map map of the field
     * @param fieldName field name
     * @return
     */
//    public static LanguageValueFieldMap getValueFromLanguageMap(Map<String, List<String>> map, String fieldName, FullBean bean) {
//        // get non-language tagged values only
//        List<String> defValues = new ArrayList<>();
//        if (!map.isEmpty() && map.containsKey(Language.DEF)) {
//            List<String> values = map.get(Language.DEF);
//            // check if there is any other language present in the map and
//            // if yes, then check if lang-tagged values already have the def tagged values present
//            if (LanguageDetectionUtils.mapHasOtherLanguagesThanDef(map.keySet())) {
//                defValues.addAll(LanguageDetectionUtils.removeLangTaggedValuesFromDef(map));
//            } else {
//                defValues.addAll(values);
//            }
//        }
//        // resolve the uri's and if contextual entity present get the preflabel
//        List<String> resolvedNonLangTaggedValues = checkForUrisAndGetPrefLabel(bean, defValues);
//
//        //  Check if the value contains at least 1 unicode letter or number (otherwise ignore)
//        List<String> cleanDefValues = filterValuesWithAtleastOneUnicodeOrNumber(resolvedNonLangTaggedValues);
//
//        if (!cleanDefValues.isEmpty()) {
//            return new LanguageValueFieldMap(fieldName, Language.DEF, cleanDefValues);
//        }
//        return null;
//    }

    public static List<String> filterValuesWithAtleastOneUnicodeOrNumber(List<String> valuesToFilter) {
       return valuesToFilter.stream().filter(value -> unicodeNumberPattern.matcher(value).find()).collect(Collectors.toList());
    }

//    private static List<String> checkForUrisAndGetPrefLabel(FullBean bean, List<String> nonLanguageTaggedValues) {
//        List<String> resolvedNonLangTaggedValues = new ArrayList<>();
//        if( !nonLanguageTaggedValues.isEmpty()) {
//            for (String value : nonLanguageTaggedValues) {
//                if (EuropeanaUriUtils.isUri(value)) {
//                    ContextualClass entity = BaseRecordService.entityExistsWithUrl(bean, value);
//                    // For uri who have contextual entity we add the prefLabels only if non-language tagged values are present.
//                    // We ignore the prefLabels if language tagged values are present.
//                    // Also, ignore the other uri values whose entity doesn't exist
//                    if (entity != null) {
//                        // preflabels here will either have "def" values (only if there was no other language value present) OR will be empty
//                        List<String> preflabels = getPrefLabelofEntity(entity, bean.getAbout());
//                        resolvedNonLangTaggedValues.addAll(preflabels);
//                    }
//                } else {
//                    resolvedNonLangTaggedValues.add(value); // add other texts as it is
//                }
//            }
//        }
//        // remove duplicates and return values
//        ComparatorUtils.removeDuplicates(resolvedNonLangTaggedValues);
//        return resolvedNonLangTaggedValues;
//    }

    /**
     * Checks if map contains keys other than "def"
     * @param keyset
     * @return
     */
    public static boolean mapHasOtherLanguagesThanDef(Set<String> keyset) {
        Set<String> copy = new HashSet<>(keyset); // deep copy
        copy.remove(Language.DEF);
        return !copy.isEmpty();
    }

    /**
     * Remove the lang-tagged values from "def"
     *
     * ex if map has values : {def=["paris", "budapest" , "venice"], en=["budapest"]}
     * then returns : ["paris", "venice"]
     * @param map
     * @return
     */
    public static List<String> removeLangTaggedValuesFromDef(Map<String, List<String>> map) {
        List<String> nonLangTaggedDefvalues = new ArrayList<>(map.get(Language.DEF));
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (!entry.getKey().equals(Language.DEF)) {
                nonLangTaggedDefvalues.removeAll(entry.getValue());
            }
        }
        return nonLangTaggedDefvalues;
    }
}
