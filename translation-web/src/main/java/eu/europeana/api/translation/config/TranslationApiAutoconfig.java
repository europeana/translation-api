package eu.europeana.api.translation.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import eu.europeana.api.commons.config.i18n.I18nService;
import eu.europeana.api.commons.config.i18n.I18nServiceImpl;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.translation.definitions.service.LanguageDetectionService;
import eu.europeana.api.translation.definitions.service.TranslationService;
import eu.europeana.api.translation.definitions.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.definitions.service.exception.TranslationServiceConfigurationException;
import eu.europeana.api.translation.service.google.GoogleLangDetectService;
import eu.europeana.api.translation.service.google.GoogleTranslationService;
import eu.europeana.api.translation.service.google.GoogleTranslationServiceClientWrapper;
import eu.europeana.api.translation.service.pangeanic.PangeanicLangDetectService;
import eu.europeana.api.translation.service.pangeanic.PangeanicTranslationService;
import eu.europeana.api.translation.web.service.DummyLangDetectService;
import eu.europeana.api.translation.web.service.DummyTranslationService;

@Configuration()
@PropertySources({@PropertySource("classpath:translation.properties"),
@PropertySource(value = "translation.user.properties", ignoreResourceNotFound = true)})
public class TranslationApiAutoconfig implements ApplicationListener<ApplicationStartedEvent>{
  
  @Value("${use.dummy.services:false}")
  private boolean useDummyServices;

  private final TranslationConfig translationConfig;
  TranslationServiceProvider translationServiceConfigProvider;
  private final Logger logger = LogManager.getLogger(TranslationApiAutoconfig.class);
  

  public TranslationApiAutoconfig(@Autowired TranslationConfig translationConfig) {
    this.translationConfig = translationConfig;
  }

  @Bean(BeanNames.BEAN_CLIENT_DETAILS_SERVICE)
  public EuropeanaClientDetailsService getClientDetailsService() {
    EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
    clientDetailsService.setApiKeyServiceUrl(translationConfig.getApiKeyUrl());
    return clientDetailsService;
  }

  @Bean(BeanNames.BEAN_I18N_SERVICE)
  public I18nService getI18nService() {
    return new I18nServiceImpl();
  }

  @Bean("messageSource")
  public MessageSource getMessageSource() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding("utf-8");
    messageSource.setDefaultLocale(Locale.ENGLISH);
    return messageSource;
  }
  
  /**
   * Creates a new client wrapper that can send translation requests to Google Cloud Translate. Note that
   * the client needs to be closed when it's not used anymore
   * @throws IOException 
   */
  @Bean(BeanNames.BEAN_GOOGLE_TRANSLATION_CLIENT_WRAPPER)
  public GoogleTranslationServiceClientWrapper getGoogleTranslationServiceClientWrapper() throws IOException {
    return new GoogleTranslationServiceClientWrapper(translationConfig.getGoogleTranslateProjectId(), translationConfig.useGoogleHttpClient());
  }

  @Bean(BeanNames.BEAN_PANGEANIC_LANG_DETECT_SERVICE)
  public PangeanicLangDetectService getPangeanicLangDetectService() {
    return new PangeanicLangDetectService(translationConfig.getPangeanicDetectEndpoint());
  }

  @Bean(BeanNames.BEAN_PANGEANIC_TRANSLATION_SERVICE)
  public PangeanicTranslationService getPangeanicTranslationService(
      @Qualifier(BeanNames.BEAN_PANGEANIC_LANG_DETECT_SERVICE) PangeanicLangDetectService pangeanicLangDetectService) {
    return new PangeanicTranslationService(translationConfig.getPangeanicTranslateEndpoint(),
        pangeanicLangDetectService);
  }

  @Bean(BeanNames.BEAN_GOOGLE_LANG_DETECT_SERVICE)
  public GoogleLangDetectService getGoogleLangDetectService(
      @Qualifier(BeanNames.BEAN_GOOGLE_TRANSLATION_CLIENT_WRAPPER) GoogleTranslationServiceClientWrapper googleTranslationServiceClientWrapper) {
    return new GoogleLangDetectService(translationConfig.getGoogleTranslateProjectId(), googleTranslationServiceClientWrapper);
  }

  @Bean(BeanNames.BEAN_GOOGLE_TRANSLATION_SERVICE)
  public GoogleTranslationService getGoogleTranslationService(
      @Qualifier(BeanNames.BEAN_GOOGLE_TRANSLATION_CLIENT_WRAPPER) GoogleTranslationServiceClientWrapper googleTranslationServiceClientWrapper) {
    return new GoogleTranslationService(translationConfig.getGoogleTranslateProjectId(), googleTranslationServiceClientWrapper);
  }
  
  @Bean(BeanNames.BEAN_DUMMY_LANG_DETECT_SERVICE)
  @ConditionalOnProperty(value="use.dummy.services", havingValue = "true")
  public DummyLangDetectService getDummyLangDetectService() {
    return new DummyLangDetectService();
  }
  
  @Bean(BeanNames.BEAN_DUMMY_TRANSLATION_SERVICE)
  @ConditionalOnProperty(value="use.dummy.services", havingValue = "true")
  public DummyTranslationService getDummyTranslationService() {
    return new DummyTranslationService();
  }

  @Bean(BeanNames.BEAN_SERVICE_PROVIDER)
  @DependsOn(value = {BeanNames.BEAN_PANGEANIC_LANG_DETECT_SERVICE,
      BeanNames.BEAN_PANGEANIC_TRANSLATION_SERVICE, BeanNames.BEAN_GOOGLE_TRANSLATION_SERVICE})
  public TranslationServiceProvider getTranslationServiceProvider() {
    this.translationServiceConfigProvider = new TranslationServiceProvider();
    // failing as the service beans are not initialized yet, would need to think of another way to
    // call this initialization
    // translationServiceConfigProvider#initTranslationServicesConfiguration;
    return translationServiceConfigProvider;
  }

  @Override
  public void onApplicationEvent(ApplicationStartedEvent event) {
    // TODO Auto-generated method stub
    // log beans for debuging purposes
    if (logger.isDebugEnabled()) {
      printRegisteredBeans(event.getApplicationContext());
    }
    
    //load either normal or dummy services (used for stress testing)
    if(! useDummyServices) {
      loadServices(event);
    }
    else {
      loadDummyServices(event);
    }
  }
  
  private void loadServices(ApplicationStartedEvent event) {
    try {
      // verify required configurations for initialization of translation services
      verifyMandatoryProperties(event.getApplicationContext());
      
      // init translation services
      initTranslationServices(event.getApplicationContext());
     } catch (Exception e) {
       // gracefully stop the application in case of configuration problems (code 1 means exception
       // occured at startup)
       logger.fatal(
           "Stopping application. Translation Service initialization failed due to configuration errors!",
           e);
       System.exit(SpringApplication.exit(event.getApplicationContext(), () -> 1));
     }
  }
  
  private void loadDummyServices(ApplicationStartedEvent event) {
    try {
      TranslationServiceProvider translationServiceProvider =
          (TranslationServiceProvider) event.getApplicationContext().getBean(BeanNames.BEAN_SERVICE_PROVIDER);
      
      //needed for the validation of the supported languages in the requests
      translationServiceProvider.readServiceConfigurations();
      
      TranslationService dummyTranslService = (TranslationService) event.getApplicationContext()
            .getBean(BeanNames.BEAN_DUMMY_TRANSLATION_SERVICE, DummyTranslationService.class);
      translationServiceProvider.getTranslationServices().put(dummyTranslService.getServiceId(), dummyTranslService);

      LanguageDetectionService dummyLangDetectService = (LanguageDetectionService) event.getApplicationContext()
          .getBean(BeanNames.BEAN_DUMMY_LANG_DETECT_SERVICE, DummyLangDetectService.class);
      translationServiceProvider.getLangDetectServices().put(dummyLangDetectService.getServiceId(), dummyLangDetectService);
    }
    catch (Exception e) {
      // gracefully stop the application in case of configuration problems (code 1 means exception
      // occured at startup)
      logger.fatal(
          "Stopping application. Dummy Translation Service initialization failed due to configuration errors or unavailable dummy service beans!",
          e);
      System.exit(SpringApplication.exit(event.getApplicationContext(), () -> 1));
    }
    
  }
  
  public void initTranslationServices(ApplicationContext ctx) throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
      TranslationServiceProvider translationServiceProvider =
          (TranslationServiceProvider) ctx.getBean(BeanNames.BEAN_SERVICE_PROVIDER);
      translationServiceProvider.initTranslationServicesConfiguration();
  }

  public void verifyMandatoryProperties(ApplicationContext ctx) {
      TranslationConfig translationConfig = (TranslationConfig) ctx.getBean(BeanNames.BEAN_TRANSLATION_CONFIG);
      translationConfig.verifyRequiredProperties();
  }
  
  private void printRegisteredBeans(ApplicationContext ctx) {
    String[] beanNames = ctx.getBeanDefinitionNames();
    Arrays.sort(beanNames);
    logger.debug("Instantiated beans:");
    logger.debug(StringUtils.join(beanNames, "\n"));
  }

}
