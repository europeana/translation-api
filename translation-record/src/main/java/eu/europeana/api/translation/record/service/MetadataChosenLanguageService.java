package eu.europeana.api.translation.record.service;

import eu.europeana.api.translation.definitions.language.Language;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europeana.api.translation.record.service.BaseService.*;

public class MetadataChosenLanguageService {

    private static final Logger LOG = LogManager.getLogger(MetadataChosenLanguageService.class);

    /**
     * Will fetch the most representative langiuage from all the proxies,  aka - chosen language
     * @param bean
     */
    public String getMostRepresentativeLanguage(FullBean bean, String targetLanguage) {
        Map<String, Integer> langCountMap = new HashMap<>();
        List<? extends Proxy> proxies = bean.getProxies();

        for (Proxy proxy : proxies) {
            ReflectionUtils.doWithFields(proxy.getClass(), field -> getLanguageAndCount(proxy, field, langCountMap, targetLanguage), BaseService.proxyFieldFilter);
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
            Optional<String> langWithHigherPrecedance =  BaseService.PRECENDANCE_LIST.stream().filter(languagesWithMostvalues :: contains).findFirst();
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
        Map<String, List<String>> langValueMap = BaseService.getValueOfTheField(proxy, false).apply(field.getName());
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
        return !(StringUtils.equals(lang, Language.NO_LINGUISTIC_CONTENT) || StringUtils.equals(lang, Language.DEF));
                // TODO figure out how to get supported language
              //  && translationService.isSupported(lang, targetLanguage);
    }

}
