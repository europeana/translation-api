package eu.europeana.api.translation.client.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.translation.client.web.TranslationApiClient;
import eu.europeana.api.translation.definitions.language.Language;
import eu.europeana.api.translation.definitions.language.LanguageValueFieldMap;
import eu.europeana.api.translation.client.utils.LanguageDetectionUtils;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

public class MetadataLangDetectionService extends BaseService {

    private static final Logger LOG = LogManager.getLogger(MetadataLangDetectionService.class);

    public MetadataLangDetectionService(TranslationApiClient translationApiClient) {
        super(translationApiClient);
    }

    /**
     * If does not match any of the languages Europeana supports or
     * if not supported by the language detection endpoint (ie. calling the isSupported method)
     * then there will be no hint supplied (this means that ‘mul’ is ignored)
     *
     * @param bean
     * @return
     */
    private String getHintForLanguageDetect(FullBean bean) {
        List<Language> edmLanguages = LanguageDetectionUtils.getEdmLanguage(bean);
        if (!edmLanguages.isEmpty()) {
            String edmLang = edmLanguages.get(0).name().toLowerCase(Locale.ROOT);
          //  if (getTranslationApiClient().isSupported(edmLang)) { TODO figure out a way to get supported lanaguages, for now true always
            if (true) {
                LOG.debug("For record {}, hint for lang-detection is {} ", bean.getAbout(), edmLang);
                return edmLang;
            } else {
                LOG.debug("For record {}, edmLanguage - {} , is NOT supported by lang detection service", bean.getAbout(), edmLang);

            }
        }
        return null;
    }

    /**
     * Gather all non-language tagged values (for all whitelisted properties) of the (non-Europeana) Proxies
     * NOTE :: Only if there isn't a language tagged value already spelled exactly the same
     *
     * Run through language detection (ie. call lang detect method) and assign (or correct) language attributes for the values
     *
     * Responses indicating that the language is not supported or the inability to recognise the language should
     * retain the language attribute provided in the source
     *
     * Add all corrected language attributes to the Europeana Proxy (duplicating the value and assigning the new language attribute)
     *
     * @param bean
     * @throws EuropeanaApiException
     * @throws LanguageDetectionException
     */
    public FullBean detectLanguageForProxy(FullBean bean) throws EuropeanaApiException {
        long start = System.currentTimeMillis();
        List<Proxy> proxies = new ArrayList<>(bean.getProxies()); // make sure we clone first so we can edit the list to our needs.

        // Data/santity check
        if (proxies.size() < 2) {
            LOG.error("Unexpected data - expected at least 2 proxies, but found only {}!", proxies.size());
            return bean;
        }
        String langHint = getHintForLanguageDetect(bean);

        // remove europeana proxy from the list
        eu.europeana.corelib.definitions.edm.entity.Proxy europeanaProxy = getEuropeanaProxy(proxies, bean.getAbout());
        proxies.remove(europeanaProxy);

        // 1. gather all the "def" values for the whitelisted fields
        for (Proxy proxy : proxies) {
            List<LanguageValueFieldMap> langValueFieldMapForDetection = new ArrayList<>();

            ReflectionUtils.doWithFields(proxy.getClass(), field -> {
                LanguageValueFieldMap fieldValuesLanguageMap = getProxyFieldsValues(proxy, field, bean);
                if (fieldValuesLanguageMap != null) {
                    langValueFieldMapForDetection.add(fieldValuesLanguageMap);
                }

            }, proxyFieldFilter);

            LOG.debug("For record {} gathered {} fields non-language tagged values for detection. ", bean.getAbout(), langValueFieldMapForDetection.size());

            Map<String, Integer> textsPerField = new LinkedHashMap<>(); // to maintain the order of the fields
            List<String> textsForDetection = new ArrayList<>();

            // 3. collect all the values in one list for single lang-detection request per proxy
            LanguageDetectionUtils.getTextsForDetectionRequest(textsForDetection, textsPerField, langValueFieldMapForDetection);

            LOG.debug("Gathering detection values for {} took {}ms ", bean.getAbout(), (System.currentTimeMillis() - start));

            // 4. send lang-detect request
            List<String> detectedLanguages = getTranslationApiClient().detectLang(createLangDetectRequest(textsForDetection, langHint)).getLangs();
            LOG.debug("Detected languages - {} ", detectedLanguages);

            // if only nulls , nothing is detected. no need to process further
            if (LanguageDetectionUtils.onlyNulls(detectedLanguages)) {
                return bean;
            }

            //5. assign language attributes to the values. This map may contain "def" tag values.
            // As for the unidentified languages or unacceptable threshold values the service returns null
            // and the source value is retained which is "def" in our case
            List<LanguageValueFieldMap> correctLangValueMap = LanguageDetectionUtils.getLangDetectedFieldValueMap(textsPerField, detectedLanguages, textsForDetection);

            // 6. add all the new language tagged values to europeana proxy
            Proxy europeanProxy = getEuropeanaProxy(bean.getProxies(), bean.getAbout());
            updateProxy(europeanProxy, correctLangValueMap);
            LOG.debug("Language detection took {}ms", bean.getAbout(), (System.currentTimeMillis() - start));
        }
        return bean;
    }

    private LanguageValueFieldMap getProxyFieldsValues(Proxy proxy, Field field, FullBean bean) {
        HashMap<String, List<String>> origFieldData = (HashMap<String, List<String>>) getValueOfTheField(proxy, false).apply(field.getName());
        return LanguageDetectionUtils.getValueFromLanguageMap(SerializationUtils.clone(origFieldData), field.getName(), bean);
    }

    private LangDetectRequest createLangDetectRequest(List<String> textsForDetection, String langHint) {
        LangDetectRequest langDetectRequest = new LangDetectRequest();
        langDetectRequest.setText(textsForDetection);
        langDetectRequest.setLang(langHint);
        return langDetectRequest;
    }

    /**
     * Updates the proxy object field values by adding the new map values
     *
     * NOTE : Only add language tagged values.
     * @param proxy
     * @param correctLangMap
     */
    private void updateProxy( Proxy proxy, List<LanguageValueFieldMap> correctLangMap) {
        correctLangMap.stream().forEach(value -> {
            Map<String, List<String>> map = getValueOfTheField(proxy, true).apply(value.getFieldName());
            // Now add the new lang-value map in the proxy
            for (Map.Entry<String, List<String>> entry : value.entrySet()) {
                if (!StringUtils.equals(entry.getKey(), Language.DEF)) {
                    if (map.containsKey(entry.getKey())) {
                        map.get(entry.getKey()).addAll(entry.getValue());
                    } else {
                        map.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        });
    }
}
