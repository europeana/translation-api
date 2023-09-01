package eu.europeana.api.translation.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.config.services.DetectServiceCfg;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.config.services.TranslationMappingCfg;
import eu.europeana.api.translation.config.services.TranslationServiceCfg;
import eu.europeana.api.translation.config.services.TranslationServicesConfiguration;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;

public class TranslationServiceConfigProvider {

  @Autowired
  ApplicationContext applicationContext;
  public static final String DEFAULT_SERVICE_CONFIG_FILE =
      "/translation_service_configuration.json";
  private final String serviceConfigFile;

  // private static final Logger logger = LogManager.getLogger(TranslationApp.class);

  TranslationServicesConfiguration translationServicesConfig;
  Map<String, LanguageDetectionService> langDetectServices = new HashMap<>();
  Map<String, TranslationService> translationServices = new HashMap<>();

  public TranslationServiceConfigProvider() {
    this(DEFAULT_SERVICE_CONFIG_FILE);
  }

  public TranslationServiceConfigProvider(String serviceConfigFile) {
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

  public void initTranslationServicesConfiguration()
      throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
    try {
      // init translation services
      readTranslationServicesConfig();
      validateTranslationServicesConfig();
    } catch (JsonProcessingException e) {
      throw new TranslationServiceConfigurationException(
          "Invalid Service Configurations, check json syntax!", e);
    }
  }

  private void validateTranslationServicesConfig() throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
    validateDetectServiceCfg();
    validateTranslationServiceCfg();
  }

  private void validateTranslationServiceCfg() throws TranslationServiceConfigurationException {
    /*
     * Validate translation config
     */
    validateTranslationServices();
    // check that a default service id is a valid one
    validateDefaultTranslationService();
    List<String> allMappingsLangPairs = validateLanguageMappings();
    // validate all languages from the supported section are actually supported
    validateSupportedLanguagePairs(allMappingsLangPairs);
  }

  private void validateSupportedLanguagePairs(List<String> allMappingsLangPairs)
      throws TranslationServiceConfigurationException {
    for (TranslationLangPairCfg langPair : translationServicesConfig.getTranslationConfig()
        .getSupported()) {
      for (String srcLang : langPair.getSrcLang()) {
        for (String trgLang : langPair.getTargetLang()) {
          if (!srcLang.equals(trgLang)
              && !allMappingsLangPairs
                  .contains(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang)
              && !translationServices
                  .get(translationServicesConfig.getTranslationConfig().getDefaultServiceId())
                  .isSupported(srcLang, trgLang)) {
            throw new TranslationServiceConfigurationException(
                "The translation services do not support all languages declared in the supported section.");
          }
        }
      }
    }
  }

  private void validateDefaultTranslationService() throws TranslationServiceConfigurationException {
    if (!translationServices
        .containsKey(translationServicesConfig.getTranslationConfig().getDefaultServiceId())) {
      throw new TranslationServiceConfigurationException(
          "Translation default service id is invalid.");
    }
  }

  private void validateTranslationServices() throws TranslationServiceConfigurationException {
    for (TranslationServiceCfg translServiceConfig : translationServicesConfig
        .getTranslationConfig().getServices()) {
      // validate unique service ids
      if (translationServices.containsKey(translServiceConfig.getId())) {
        throw new TranslationServiceConfigurationException(
            "Duplicate service id in the translation config.");
      }
      TranslationService translService;
      try {
        translService = (TranslationService) applicationContext
            .getBean(Class.forName(translServiceConfig.getClassname()));
      } catch (BeansException | ClassNotFoundException e) {
        throw new TranslationServiceConfigurationException(
            "Service bean not available: " + translServiceConfig.getClassname(), e);
      }

      translationServices.put(translServiceConfig.getId(), translService);
    }
  }

  private List<String> validateLanguageMappings() throws TranslationServiceConfigurationException {
    // validate that each service supports the languages declared in the mappings section
    List<String> allMappingsLangPairs = new ArrayList<>();
    for (TranslationMappingCfg translMapping : translationServicesConfig.getTranslationConfig()
        .getMappings()) {
      if (translationServices.get(translMapping.getServiceId()) == null) {
        throw new TranslationServiceConfigurationException(
            "Translation service id declared in the mappings is invalid.");
      }
      for (String srcLang : translMapping.getSrcLang()) {
        for (String trgLang : translMapping.getTrgLang()) {
          if (!srcLang.equals(trgLang) && !translationServices.get(translMapping.getServiceId())
              .isSupported(srcLang, trgLang)) {
            throw new TranslationServiceConfigurationException("Translation service: "
                + translMapping.getServiceId() + ", does not support the language pair: " + srcLang
                + TranslationAppConstants.LANG_DELIMITER + trgLang
                + ", declared in the mappings section.");
          }
          allMappingsLangPairs.add(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang);
        }
      }
    }
    return allMappingsLangPairs;
  }

  private void validateDetectServiceCfg() throws LangDetectionServiceConfigurationException {
    /*
     * Validate lang detection config
     */
    for (DetectServiceCfg detectServiceCfg : translationServicesConfig.getLangDetectConfig()
        .getServices()) {
      // validate unique service ids
      if (langDetectServices.containsKey(detectServiceCfg.getId())) {
        throw new LangDetectionServiceConfigurationException(
            "Duplicate service id in the language detection config.");
      }
      LanguageDetectionService detectService;
      try {
        detectService = (LanguageDetectionService) applicationContext
            .getBean(Class.forName(detectServiceCfg.getClassname()));
      } catch (BeansException | ClassNotFoundException e) {
        throw new LangDetectionServiceConfigurationException(
            "Service bean not available: " + detectServiceCfg.getClassname(), e);
      }
      langDetectServices.put(detectServiceCfg.getId(), detectService);
    }
    // check that a default service id is a valid one
    if (!langDetectServices
        .containsKey(translationServicesConfig.getLangDetectConfig().getDefaultServiceId())) {
      throw new LangDetectionServiceConfigurationException(
          "Language detection default service id is invalid.");
    }
    // validate that the default service supports all languages from the supported section
    for (String supportedLang : translationServicesConfig.getLangDetectConfig().getSupported()) {
      if (!langDetectServices
          .get(translationServicesConfig.getLangDetectConfig().getDefaultServiceId())
          .isSupported(supportedLang)) {
        throw new LangDetectionServiceConfigurationException(
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
