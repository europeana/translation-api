package eu.europeana.api.translation.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import eu.europeana.api.translation.service.pangeanic.PangeanicTranslationService;

/**
 * Class used to read the translation service configurations, validate them, initialize mapping for
 * language detection and translation services
 * 
 * @author GordeaS
 *
 */
public class TranslationServiceProvider extends AbstractServiceInstantiationUtils{

  public static final String DEFAULT_SERVICE_CONFIG_FILE =
      "/translation_service_configuration.json";
  private static final String FILE_PANGEANIC_LANGUAGE_THRESHOLDS =
      "pangeanic_language_thresholds.properties";
  
  private final Logger logger = LogManager.getLogger(TranslationServiceProvider.class);

  private final String serviceConfigLocation;
  private final File serviceConfigFile;

  @Autowired
  ApplicationContext applicationContext;

  @Resource(name = BeanNames.BEAN_TRANSLATION_CONFIG)
  TranslationConfig translationConfig;
  
  @Resource(name = BeanNames.BEAN_LANGDETECT_PRE_PROCESSOR_SERVICE)
  LanguageDetectionService languageDetectionPreProcessor;

  @Resource(name = BeanNames.BEAN_TRANSLATION_PRE_PROCESSOR_SERVICE)
  TranslationService translationServicePreProcessor;

  Map<String, LanguageDetectionService> langDetectServices = new ConcurrentHashMap<>();
  Map<String, TranslationService> translationServices = new ConcurrentHashMap<>();
  Map<String, TranslationService> langMappings4TranslateServices = new ConcurrentHashMap<>();
  
  TranslationServicesConfiguration translationServicesConfig;

  /**
   * Default contructor using default config file
   */
  public TranslationServiceProvider() {
    this(DEFAULT_SERVICE_CONFIG_FILE);
  }

  /**
   * Constructor using a config file available in resources
   * 
   * @param serviceConfigLocation a config file available in classpath
   */
  public TranslationServiceProvider(String serviceConfigLocation) {
    this.serviceConfigLocation = serviceConfigLocation;
    this.serviceConfigFile = null;
  }

  /**
   * Constructor using a file available on file system
   * 
   * @param serviceConfigFile a config file available in classpath
   */
  public TranslationServiceProvider(File serviceConfigFile) {
    this.serviceConfigFile = serviceConfigFile;
    this.serviceConfigLocation = null;
  }

  public Optional<DetectServiceCfg> getLangDetectServiceDefinition(String beanName) {
    // if configuration not available
    if (translationServicesConfig == null
        || translationServicesConfig.getLangDetectConfig() == null) {
      return Optional.empty();
    }

    List<DetectServiceCfg> serviceDefinitions =
        translationServicesConfig.getLangDetectConfig().getServiceDefinition();
    return serviceDefinitions.stream().filter(sd -> beanName.equals(sd.getId())).findFirst();
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
   * 
   * @throws TranslationServiceConfigurationException
   */
  void readServiceConfigurations() throws TranslationServiceConfigurationException {
    if (Objects.nonNull(serviceConfigFile)) {
      // deployments should provide config files in the external configurations folder
      readServiceConfigurationsFromConfigFile();
    } else {
      // mainly for integration testing purposes
      readServiceConfigurationsFromClassPath();
    }
  }

  private void readServiceConfigurationsFromClassPath()
      throws TranslationServiceConfigurationException {
    try (InputStream inputStream = getClass().getResourceAsStream(getServiceConfigLocation())) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      parseTranslationServicesConfig(reader);

      if (logger.isInfoEnabled()) {
        logger.info("Successfully loaded service configurations from classpath resources.");
      }
    } catch (IOException e) {
      throw new TranslationServiceConfigurationException(
          "Cannot read service configurations from classpath resources!", e);
    }
  }

  private void readServiceConfigurationsFromConfigFile()
      throws TranslationServiceConfigurationException {
    try (BufferedReader input = Files.newBufferedReader(serviceConfigFile.toPath())) {
      parseTranslationServicesConfig(input);
      if (logger.isInfoEnabled()) {
        logger.info("Successfully loaded service configurations from external config file.");
      }
    } catch (IOException e) {
      throw new TranslationServiceConfigurationException(
          "Cannot load service configurations from external config file: "
              + serviceConfigFile.toPath(),
          e);
    }
  }

  private void parseTranslationServicesConfig(BufferedReader reader)
      throws JsonProcessingException {
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    translationServicesConfig =
        new ObjectMapper().readValue(content, TranslationServicesConfiguration.class);
  }

  private void validateAndInitServices()
      throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
    
    // init lang detection services
    validateAndInitLangDetectionServices();
    validateDefaultLangDetectServiceConfig();
    
    // init translation services
    validateAndInitFromTranslationServiceCfg();
  }

  private void validateAndInitFromTranslationServiceCfg() throws TranslationServiceConfigurationException {
    /*
     * Validate translation config
     */
    validateAndInitTranslationServices();
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

  private void validateSupportedLanguagePair(@NotNull String srcLang, @NotNull String trgLang)
      throws TranslationServiceConfigurationException {

    // check if available in language mappings
    boolean isSupported =
        getLangMappings4TranslateServices().containsKey(LanguagePair.generateKey(srcLang, trgLang));
    if (!isSupported && getDefaultTranslationService() != null) {
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

  private void validateAndInitTranslationServices() throws TranslationServiceConfigurationException {
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
      
      boolean isPangeanic = translService.getClass().equals(PangeanicTranslationService.class);
      if(isPangeanic) {
        ((PangeanicTranslationService) translService).init(
            loadPangeanicTranslationThresholds(FILE_PANGEANIC_LANGUAGE_THRESHOLDS));
      }
      
      getTranslationServices().put(translServiceConfig.getId(), translService);
    }
  }

  private void validateAndInitLanguageMappings() throws TranslationServiceConfigurationException {
    // validate that each service supports the languages declared in the mappings
    // section
    if (translationServicesConfig.getTranslationConfig().getMappings() == null) {
      // nothing to validate
      return;
    }

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



  private void validateDefaultLangDetectServiceConfig()
      throws LangDetectionServiceConfigurationException {
    final String defaultServiceId =
        translationServicesConfig.getLangDetectConfig().getDefaultServiceId();
    if (!getLangDetectServices().containsKey(defaultServiceId)) {
      throw new LangDetectionServiceConfigurationException(
          "Language detection default service id is invalid.");
    }

    // validate that the default service supports all languages from the supported
    // section
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

  private void validateAndInitLangDetectionServices()
      throws LangDetectionServiceConfigurationException {
    // validate and instantiate all services
    for (DetectServiceCfg detectServiceCfg : translationServicesConfig.getLangDetectConfig()
        .getServiceDefinition()) {
      // validate unique service ids
      if (getLangDetectServices().containsKey(detectServiceCfg.getId())) {
        throw new LangDetectionServiceConfigurationException(
            "Duplicate service id in the language detection config.");
      }
      // find pre-registered bean
      LanguageDetectionService detectService;
      
      try {
        final Class<?> beanClass = Class.forName(detectServiceCfg.getClassname());
        Map<String, ?> beansOfType = applicationContext.getBeansOfType(beanClass);
        if (beansOfType.size() == 1) {
          detectService = (LanguageDetectionService) applicationContext.getBean(beanClass);
        } else {
          detectService = (LanguageDetectionService) beansOfType.get(detectServiceCfg.getId());
        }
      } catch (BeansException | ClassNotFoundException e) {
        throw new LangDetectionServiceConfigurationException(
            "Service bean not available: " + detectServiceCfg.getClassname(), e);
      }
      //set service ID
      detectService.setServiceId(detectServiceCfg.getId());
      //set threshold configurations
      if (StringUtils.isNotEmpty(detectServiceCfg.getConfigFilePath())) {
        detectService.setThresholdsConf(loadLanguageDetectionThresholds(detectServiceCfg));
      }
      //instantiate referenced services
      if(detectServiceCfg.getReferencedServices() != null) {
        List<LanguageDetectionService> referencedServices = new ArrayList<>(detectServiceCfg.getReferencedServices().size());
        for (DetectServiceCfg referencedServiceCfg : detectServiceCfg.getReferencedServices()) {
          referencedServices.add(createServiceInstance(referencedServiceCfg));
        }
        detectService.setReferencedServices(referencedServices);
      }
      
      // add bean to service map
      getLangDetectServices().put(detectServiceCfg.getId(), detectService);
    }
  }
  
  public String getServiceConfigLocation() {
    return serviceConfigLocation;
  }

  public Map<String, TranslationService> getLangMappings4TranslateServices() {
    return langMappings4TranslateServices;
  }

  public LanguageDetectionService getLanguageDetectionPreProcessor() {
    return languageDetectionPreProcessor;
  }

  public TranslationService getTranslationServicePreProcessor() {
    return translationServicePreProcessor;
  }

  TranslationConfig getTranslationConfig() {
    return translationConfig;
  }

  public TranslationServicesConfiguration getTranslationServicesConfig() {
    return translationServicesConfig;
  }
}
