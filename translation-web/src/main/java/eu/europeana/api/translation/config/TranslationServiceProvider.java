package eu.europeana.api.translation.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.config.serialization.DetectServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationLangPairCfg;
import eu.europeana.api.translation.config.serialization.TranslationMappingCfg;
import eu.europeana.api.translation.config.serialization.TranslationServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationServicesConfiguration;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.web.exception.TranslationException;

@Component(BeanNames.BEAN_SERVICE_CONFIG_PROVIDER)
public class TranslationServiceProvider{

  @Autowired
  ApplicationContext applicationContext;
  public static final String DEFAULT_SERVICE_CONFIG_FILE = "/service_configuration.json";
  private final String serviceConfigFile;

  TranslationServicesConfiguration translationServicesConfig;
  Map<String, LanguageDetectionService> langDetectServices = new HashMap<>();
  Map<String, TranslationService> translationServices = new HashMap<>();

  public TranslationServiceProvider() {
    this(DEFAULT_SERVICE_CONFIG_FILE);
  }

  public TranslationServiceProvider(String serviceConfigFile) {
    this.serviceConfigFile = serviceConfigFile; 
  }

  public TranslationServicesConfiguration getTranslationServicesConfig() {
    return translationServicesConfig;
  }

  public Map<String, LanguageDetectionService> getLangDetectServices() {
    return langDetectServices;
  }

  public Map<String, TranslationService> getTranslationServices() {
    return translationServices;
  }

//  // executed after all beans are initialized, to initialize the services from the main json config
//  // file
//  @Override
//  public void onApplicationEvent(ContextRefreshedEvent event) {
//    try {
//      initTranslationServicesConfiguration();
//    } catch (Exception e) {
//      throw new RuntimeException(
//          "The initialization of the services from the global json config has failed.", e);
//    }
//  }

  public void initTranslationServicesConfiguration() throws Exception {
    readTranslationServicesConfig();
    validateTranslationServicesConfig();
  }

  private void validateTranslationServicesConfig()
      throws TranslationException, ClassNotFoundException {

    validateDetectServiceCfg();
    validateTranslationServiceCfg();
  }

  private void validateTranslationServiceCfg() throws TranslationException, ClassNotFoundException {
    /*
     * Validate translation config
     */
    for (TranslationServiceCfg translServiceConfig : translationServicesConfig.getTranslConfig()
        .getServices()) {
      // validate unique service ids
      if (translationServices.containsKey(translServiceConfig.getId())) {
        throw new TranslationException("Duplicate service id in the translation config.");
      }
      TranslationService translService = (TranslationService) applicationContext
          .getBean(Class.forName(translServiceConfig.getClassname()));
      translationServices.put(translServiceConfig.getId(), translService);
    }
    // check that a default service id is a valid one
    if (!translationServices
        .containsKey(translationServicesConfig.getTranslConfig().getDefaultServiceId())) {
      throw new TranslationException("Translation default service id is invalid.");
    }
    // validate that each service supports the languages declared in the mappings section
    List<String> allMappingsLangPairs = new ArrayList<>();
    for (TranslationMappingCfg translMapping : translationServicesConfig.getTranslConfig()
        .getMappings()) {
      if (translationServices.get(translMapping.getServiceId()) == null) {
        throw new TranslationException(
            "Translation service id declared in the mappings is invalid.");
      }
      for (String srcLang : translMapping.getSrcLang()) {
        for (String trgLang : translMapping.getTrgLang()) {
          if (!srcLang.equals(trgLang) && !translationServices.get(translMapping.getServiceId())
              .isSupported(srcLang, trgLang)) {
            throw new TranslationException("Translation service: " + translMapping.getServiceId()
                + ", does not support the language pair: " + srcLang
                + TranslationAppConstants.LANG_DELIMITER + trgLang
                + ", declared in the mappings section.");
          }
          allMappingsLangPairs.add(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang);
        }
      }
    }
    // validate all languages from the supported section are actually supported
    for (TranslationLangPairCfg langPair : translationServicesConfig.getTranslConfig()
        .getSupported()) {
      for (String srcLang : langPair.getSrcLang()) {
        for (String trgLang : langPair.getTrgLang()) {
          if (!srcLang.equals(trgLang)
              && !allMappingsLangPairs
                  .contains(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang)
              && !translationServices
                  .get(translationServicesConfig.getTranslConfig().getDefaultServiceId())
                  .isSupported(srcLang, trgLang)) {
            throw new TranslationException(
                "The translation services do not support all languages declared in the supported section.");
          }
        }
      }
    }
  }

  private void validateDetectServiceCfg() throws TranslationException, ClassNotFoundException {
    /*
     * Validate lang detection config
     */
    for (DetectServiceCfg detectServiceCfg : translationServicesConfig.getLangDetectConfig()
        .getServices()) {
      // validate unique service ids
      if (langDetectServices.containsKey(detectServiceCfg.getId())) {
        throw new TranslationException("Duplicate service id in the language detection config.");
      }
      LanguageDetectionService detectService = (LanguageDetectionService) applicationContext
          .getBean(Class.forName(detectServiceCfg.getClassname()));
      langDetectServices.put(detectServiceCfg.getId(), detectService);
    }
    // check that a default service id is a valid one
    if (!langDetectServices
        .containsKey(translationServicesConfig.getLangDetectConfig().getDefaultServiceId())) {
      throw new TranslationException("Language detection default service id is invalid.");
    }
    // validate that the default service supports all languages from the supported section
    for (String supportedLang : translationServicesConfig.getLangDetectConfig().getSupported()) {
      if (!langDetectServices
          .get(translationServicesConfig.getLangDetectConfig().getDefaultServiceId())
          .isSupported(supportedLang)) {
        throw new TranslationException(
            "The default language detection service does not support language: " + supportedLang
                + ", declared in the supported section");
      }
    }
  }

  private void readTranslationServicesConfig()
      throws JsonProcessingException, JsonMappingException {
    InputStream inputStream = getClass().getResourceAsStream(getServiceConfigFile());
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    translationServicesConfig =
        new ObjectMapper().readValue(content, TranslationServicesConfiguration.class);
  }

  public String getServiceConfigFile() {
    return serviceConfigFile;
  }

}
