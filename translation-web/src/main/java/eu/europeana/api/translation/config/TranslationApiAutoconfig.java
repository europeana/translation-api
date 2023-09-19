package eu.europeana.api.translation.config;

import java.util.Arrays;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import eu.europeana.api.commons.config.i18n.I18nService;
import eu.europeana.api.commons.config.i18n.I18nServiceImpl;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.translation.service.GoogleLangDetectService;
import eu.europeana.api.translation.service.GoogleTranslationService;
import eu.europeana.api.translation.service.GoogleTranslationServiceClientWrapper;
import eu.europeana.api.translation.service.PangeanicLangDetectService;
import eu.europeana.api.translation.service.PangeanicTranslationService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;

@Configuration()
public class TranslationApiAutoconfig implements ApplicationListener<ApplicationStartedEvent>{

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
   */
  @Bean(BeanNames.BEAN_GOOGLE_TRANSLATION_CLIENT_WRAPPER)
  public GoogleTranslationServiceClientWrapper getGoogleTranslationServiceClientWrapper() {
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
