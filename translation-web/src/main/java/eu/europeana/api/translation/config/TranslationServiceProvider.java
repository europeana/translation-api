package eu.europeana.api.translation.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.config.services.DetectServiceCfg;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.config.services.TranslationMappingCfg;
import eu.europeana.api.translation.config.services.TranslationServiceCfg;
import eu.europeana.api.translation.config.services.TranslationServicesConfiguration;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;

/**
 * Class used to read the traslation service configurations, validate them, initialize mapping for
 * language detection and translation services
 * 
 * @author GordeaS
 *
 */
public class TranslationServiceProvider {

  @Autowired
  ApplicationContext applicationContext;
  public static final String DEFAULT_SERVICE_CONFIG_FILE =
      "/translation_service_configuration.json";
  private final String serviceConfigFile;

  TranslationServicesConfiguration translationServicesConfig;
  Map<String, LanguageDetectionService> langDetectServices = new ConcurrentHashMap<>();
  Map<String, TranslationService> translationServices = new ConcurrentHashMap<>();
  Map<String, TranslationService> langMappings4TranslateServices = new ConcurrentHashMap<>();

  /**
   * Default contructor using default config file
   */
  public TranslationServiceProvider() {
    this(DEFAULT_SERVICE_CONFIG_FILE);
  }

  /**
   * Construtor using an atenitive config file
   * 
   * @param serviceConfigFile a config file available in classpath
   */
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

  /**
   * Initialization of language detection and translation services
   * 
   * @throws TranslationServiceConfigurationException if translations services are not properly
   *         configured
   * @throws LangDetectionServiceConfigurationException if language detection services are not
   *         properly configured
   */
  public void initTranslationServicesConfiguration()
      throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
    // init translation services
    readServiceConfigurations();
    validateAndInitServices();
  }

  /**
   * Method for reading and parsing service configurations 
   * @throws TranslationServiceConfigurationException
   */
  void readServiceConfigurations() throws TranslationServiceConfigurationException {
    try (InputStream inputStream = getClass().getResourceAsStream(getServiceConfigFile())) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
      translationServicesConfig =
          new ObjectMapper().readValue(content, TranslationServicesConfiguration.class);
    } catch (IOException e) {
      throw new TranslationServiceConfigurationException("Cannot read serviceConfigfile!", e);
    }
  }

  private void validateAndInitServices()
      throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
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
    // init language mappings
    validateAndInitLanguageMappings();
    // validate all languages from the supported section are actually supported
    validateSupportedLanguagePairs();
  }

  private void validateSupportedLanguagePairs() throws TranslationServiceConfigurationException {
    final List<TranslationLangPairCfg> supportedLanguagePairs =
        translationServicesConfig.getTranslationConfig().getSupported();
    for (TranslationLangPairCfg langPair : supportedLanguagePairs) {
      // iterate src lang list
      for (String srcLang : langPair.getSrcLang()) {
        // iterate src lang list
        for (String trgLang : langPair.getTargetLang()) {
          validateSupportedLanguagePair(srcLang, trgLang);
        }
      }
    }
  }

  private void validateSupportedLanguagePair(String srcLang, String trgLang)
      throws TranslationServiceConfigurationException {
    // src and taret language must be different
    if (srcLang.equals(trgLang)) {
      throw new TranslationServiceConfigurationException(
          "Invalid configuration for supported language pairs by translation service! Target language must be different from the source language: "
              + srcLang);
    }

    // check if available in language mappings
    boolean isSupported =
        langMappings4TranslateServices.containsKey(LanguagePair.generateKey(srcLang, trgLang));
    if (!isSupported) {
      // check if supported by default service
      isSupported = getDefaultTranslationService().isSupported(srcLang, trgLang);
    }

    if (!isSupported) {
      throw new TranslationServiceConfigurationException(
          "The translation services do not support all languages declared in the supported section.");
    }
  }

  private void validateDefaultTranslationService() throws TranslationServiceConfigurationException {
    if (!getTranslationServices().containsKey(getDefaultTranslationServiceId())) {
      throw new TranslationServiceConfigurationException(
          "Translation default service id is invalid.");
    }
  }

  private String getDefaultTranslationServiceId() {
    return translationServicesConfig.getTranslationConfig().getDefaultServiceId();
  }

  private TranslationService getDefaultTranslationService() {
    return getTranslationServices().getOrDefault(getDefaultTranslationServiceId(), null);
  }

  private void validateTranslationServices() throws TranslationServiceConfigurationException {
    for (TranslationServiceCfg translServiceConfig : translationServicesConfig
        .getTranslationConfig().getServices()) {
      // validate unique service ids
      if (getTranslationServices().containsKey(translServiceConfig.getId())) {
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
      translService.setServiceId(translServiceConfig.getId());
      getTranslationServices().put(translServiceConfig.getId(), translService);
    }
  }

  private void validateAndInitLanguageMappings() throws TranslationServiceConfigurationException {
    // validate that each service supports the languages declared in the mappings section
    // List<String> allMappingsLangPairs = new ArrayList<>();
    for (TranslationMappingCfg translMapping : translationServicesConfig.getTranslationConfig()
        .getMappings()) {
      final String serviceId = translMapping.getServiceId();
      final TranslationService translationService = verifyRegisteredService(serviceId);

      // register language mapping
      for (String srcLang : translMapping.getSrcLang()) {
        for (String trgLang : translMapping.getTrgLang()) {
          registerLanguageMapping(translationService, srcLang, trgLang);
        }
      }
    }
  }

  private void registerLanguageMapping(final TranslationService translationService, String srcLang,
      String trgLang) throws TranslationServiceConfigurationException {
    // for each language pair
    if (srcLang.equals(trgLang)) {
      throw new TranslationServiceConfigurationException(
          "Invalid language mapping in service configurations! Target language must be different from the source language: "
              + srcLang + " for service with id: " + translationService.getServiceId());
    }

    String key = LanguagePair.generateKey(srcLang, trgLang);
    if (!translationService.isSupported(srcLang, trgLang)) {
      throw new TranslationServiceConfigurationException(
          "Invalid service configuration! Translation service: " + translationService.getServiceId()
              + ", does not support the language pair: " + key
              + ", declared in the mappings section.");
    }

    // prevent duplicate language pair mappings
    if (getLangMappings4TranslateServices().containsKey(key)) {
      throw new TranslationServiceConfigurationException(
          "Dupplicate language mapping in service configurations for key: " + key);
    }

    getLangMappings4TranslateServices().put(key, translationService);
  }

  private TranslationService verifyRegisteredService(final String serviceId)
      throws TranslationServiceConfigurationException {
    // verify if bean is available
    final boolean isServiceBeanRegistered = getTranslationServices().containsKey(serviceId);
    if (!isServiceBeanRegistered) {
      throw new TranslationServiceConfigurationException(
          "Translation service id declared in the mappings is invalid.");
    }
    return getTranslationServices().get(serviceId);
  }

  private void validateDetectServiceCfg() throws LangDetectionServiceConfigurationException {
    /*
     * Validate lang detection config
     */
    validateDeclaredLangDetectionServices();
    // validate default lang detect service
    validateDefaultLangDetectServiceConfig();
  }

  private void validateDefaultLangDetectServiceConfig()
      throws LangDetectionServiceConfigurationException {
    final String defaultServiceId =
        translationServicesConfig.getLangDetectConfig().getDefaultServiceId();
    if (!getLangDetectServices().containsKey(defaultServiceId)) {
      throw new LangDetectionServiceConfigurationException(
          "Language detection default service id is invalid.");
    }

    // validate that the default service supports all languages from the supported section
    final LanguageDetectionService defaultLanguageDetectionService =
        getLangDetectServices().get(defaultServiceId);

    for (String supportedLang : translationServicesConfig.getLangDetectConfig().getSupported()) {
      if (!defaultLanguageDetectionService.isSupported(supportedLang)) {
        throw new LangDetectionServiceConfigurationException(
            "The default language detection service does not support language: " + supportedLang
                + ", declared in the supported section");
      }
    }
  }

  private void validateDeclaredLangDetectionServices()
      throws LangDetectionServiceConfigurationException {

    for (DetectServiceCfg detectServiceCfg : translationServicesConfig.getLangDetectConfig()
        .getServices()) {
      // validate unique service ids
      if (getLangDetectServices().containsKey(detectServiceCfg.getId())) {
        throw new LangDetectionServiceConfigurationException(
            "Duplicate service id in the language detection config.");
      }
      // find pre-registered bean
      LanguageDetectionService detectService;
      try {
        final Class<?> beanClass = Class.forName(detectServiceCfg.getClassname());
        detectService = (LanguageDetectionService) applicationContext.getBean(beanClass);
      } catch (BeansException | ClassNotFoundException e) {
        throw new LangDetectionServiceConfigurationException(
            "Service bean not available: " + detectServiceCfg.getClassname(), e);
      }
      detectService.setServiceId(detectServiceCfg.getId());
      // add bean to service map
      getLangDetectServices().put(detectServiceCfg.getId(), detectService);
    }
  }


  public String getServiceConfigFile() {
    return serviceConfigFile;
  }

  public Map<String, TranslationService> getLangMappings4TranslateServices() {
    return langMappings4TranslateServices;
  }



}
