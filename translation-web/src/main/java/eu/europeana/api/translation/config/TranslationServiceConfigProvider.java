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
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

public class TranslationServiceConfigProvider {

  @Autowired
  ApplicationContext applicationContext;
  public static final String DEFAULT_SERVICE_CONFIG_FILE =
      "/translation_service_configuration.json";
  private final String serviceConfigFile;

//  private static final Logger logger = LogManager.getLogger(TranslationApp.class);

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

  public void initTranslationServicesConfiguration(){
    try {
      // init translation services
      readTranslationServicesConfig();
      validateTranslationServicesConfig();
    }catch(JsonProcessingException | ClassNotFoundException | LanguageDetectionException e) {
      throw new RuntimeException("Invalid Service Configurations!", e);
    }
  }

  private void validateTranslationServicesConfig()
      throws LanguageDetectionException, ClassNotFoundException {

    validateDetectServiceCfg();
    validateTranslationServiceCfg();
  }

  private void validateTranslationServiceCfg()
      throws LanguageDetectionException, ClassNotFoundException {
    /*
     * Validate translation config
     */
    for (TranslationServiceCfg translServiceConfig : translationServicesConfig
        .getTranslationConfig().getServices()) {
      // validate unique service ids
      if (translationServices.containsKey(translServiceConfig.getId())) {
        throw new LanguageDetectionException("Duplicate service id in the translation config.");
      }
      TranslationService translService = (TranslationService) applicationContext
          .getBean(Class.forName(translServiceConfig.getClassname()));
      translationServices.put(translServiceConfig.getId(), translService);
    }
    // check that a default service id is a valid one
    if (!translationServices
        .containsKey(translationServicesConfig.getTranslationConfig().getDefaultServiceId())) {
      throw new LanguageDetectionException("Translation default service id is invalid.");
    }
    // validate that each service supports the languages declared in the mappings section
    List<String> allMappingsLangPairs = new ArrayList<>();
    for (TranslationMappingCfg translMapping : translationServicesConfig.getTranslationConfig()
        .getMappings()) {
      if (translationServices.get(translMapping.getServiceId()) == null) {
        throw new LanguageDetectionException(
            "Translation service id declared in the mappings is invalid.");
      }
      for (String srcLang : translMapping.getSrcLang()) {
        for (String trgLang : translMapping.getTrgLang()) {
          if (!srcLang.equals(trgLang) && !translationServices.get(translMapping.getServiceId())
              .isSupported(srcLang, trgLang)) {
            throw new LanguageDetectionException("Translation service: "
                + translMapping.getServiceId() + ", does not support the language pair: " + srcLang
                + TranslationAppConstants.LANG_DELIMITER + trgLang
                + ", declared in the mappings section.");
          }
          allMappingsLangPairs.add(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang);
        }
      }
    }
    // validate all languages from the supported section are actually supported
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
            throw new LanguageDetectionException(
                "The translation services do not support all languages declared in the supported section.");
          }
        }
      }
    }
  }

  private void validateDetectServiceCfg()
      throws LanguageDetectionException, ClassNotFoundException {
    /*
     * Validate lang detection config
     */
    for (DetectServiceCfg detectServiceCfg : translationServicesConfig.getLangDetectConfig()
        .getServices()) {
      // validate unique service ids
      if (langDetectServices.containsKey(detectServiceCfg.getId())) {
        throw new LanguageDetectionException(
            "Duplicate service id in the language detection config.");
      }
      LanguageDetectionService detectService = (LanguageDetectionService) applicationContext
          .getBean(Class.forName(detectServiceCfg.getClassname()));
      langDetectServices.put(detectServiceCfg.getId(), detectService);
    }
    // check that a default service id is a valid one
    if (!langDetectServices
        .containsKey(translationServicesConfig.getLangDetectConfig().getDefaultServiceId())) {
      throw new LanguageDetectionException("Language detection default service id is invalid.");
    }
    // validate that the default service supports all languages from the supported section
    for (String supportedLang : translationServicesConfig.getLangDetectConfig().getSupported()) {
      if (!langDetectServices
          .get(translationServicesConfig.getLangDetectConfig().getDefaultServiceId())
          .isSupported(supportedLang)) {
        throw new LanguageDetectionException(
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
