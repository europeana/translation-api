package eu.europeana.api.translation.config;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import eu.europeana.api.commons.config.i18n.I18nService;
import eu.europeana.api.commons.config.i18n.I18nServiceImpl;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.translation.service.GoogleTranslationService;
import eu.europeana.api.translation.service.PangeanicLangDetectService;
import eu.europeana.api.translation.service.PangeanicTranslationService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;

@Configuration()
public class TranslationApiAutoconfig{

  private final TranslationConfig translationConfig;
  TranslationServiceConfigProvider translationServiceConfigProvider;

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
    ReloadableResourceBundleMessageSource messageSource =  new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding("utf-8");
    messageSource.setDefaultLocale(Locale.ENGLISH);
    return messageSource;
  }
  
  @Bean(BeanNames.BEAN_PANGEANIC_LANG_DETECT_SERVICE)
  public PangeanicLangDetectService getPangeanicLangDetectService() {
    return new PangeanicLangDetectService(translationConfig.getPangeanicDetectEndpoint());
  }
  
  @Bean(BeanNames.BEAN_PANGEANIC_TRANSLATION_SERVICE)
  public PangeanicTranslationService getPangeanicTranslationService(
      @Qualifier(BeanNames.BEAN_PANGEANIC_LANG_DETECT_SERVICE) 
      PangeanicLangDetectService pangeanicLangDetectService) {
    return new PangeanicTranslationService(translationConfig.getPangeanicTranslateEndpoint(), pangeanicLangDetectService);
  }
  
  @Bean(BeanNames.BEAN_GOOGLE_TRANSLATION_SERVICE)
  public GoogleTranslationService getGoogleTranslationService() {
    final String projectId = translationConfig.getGoogleTranslateProjectId();
    //allow service mocking 
    final boolean initClientConnection = !"google-test".equals(projectId);
    return new GoogleTranslationService(projectId, initClientConnection, translationConfig.useGoogleHttpClient());
  } 
  
  
  @Bean(BeanNames.BEAN_SERVICE_CONFIG_PROVIDER)
  @DependsOn(value = {BeanNames.BEAN_PANGEANIC_LANG_DETECT_SERVICE, BeanNames.BEAN_PANGEANIC_TRANSLATION_SERVICE, BeanNames.BEAN_GOOGLE_TRANSLATION_SERVICE})
  public TranslationServiceConfigProvider getTranslationServiceConfigProvider() throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
    this.translationServiceConfigProvider = new TranslationServiceConfigProvider();
    //failing as the service beans are not initialized yet, would need to think of another way to call this initialization 
//    translationServiceConfigProvider.initTranslationServicesConfiguration();
    return translationServiceConfigProvider;
  }

}
