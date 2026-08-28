package eu.europeana.api.translation.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;
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
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.NonNull;
import eu.europeana.api.commons_sb3.error.ApiRequestPathMethodService;
import eu.europeana.api.commons_sb3.error.config.ErrorConfig;
import eu.europeana.api.commons_sb3.error.i18n.I18nService;
import eu.europeana.api.commons_sb3.error.i18n.I18nServiceImpl;
import eu.europeana.api.commons_sb3.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.translation.service.RelatedLanguages;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;
import eu.europeana.api.translation.web.exception.AppConfigurationException;
import eu.europeana.api.translation.web.model.CachedTranslation;
import eu.europeana.api.translation.web.service.LangDetectionPreProcessor;
import eu.europeana.api.translation.web.service.RedisCacheService;
import eu.europeana.api.translation.web.service.TranslationPreProcessor;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;

/**
 * Translation API configuration class
 */
@Configuration()
@PropertySource(value = "translation.user.properties", ignoreResourceNotFound = true)
public class TranslationApiAutoconfig implements ApplicationListener<ApplicationStartedEvent> {

  private final Logger logger = LogManager.getLogger(TranslationApiAutoconfig.class);

  /**
   * Any value that has at least 2 unicode consecutive letters. The condition considered the fact
   * that there can be words with only 2 letters that retain sufficient meaning and are therefore
   * reasonable to be translated, especially when looking at languages other than English (see
   * article - https://www.grammarly.com/blog/the-shortest-words-in-the-english-language/).
   */
  private static final String PATTERN = "\\p{IsAlphabetic}{2,}";
  private static final Pattern IsAlphabetic = Pattern.compile(PATTERN);

  TranslationServiceProvider translationServiceProvider;
  RelatedLanguages relatedLanguages;

  private final TranslationConfig translationConfig;

  @Value("${translation.service.config.file:}")
  private String serviceConfigFile;

  @Value("${translation.related.languages.config.file:}")
  private String relatedLanguagesConfigFile;


  /**
   * Constructor
   * 
   * @param translationConfig configuration object
   */
  public TranslationApiAutoconfig(@Autowired TranslationConfig translationConfig) {
    this.translationConfig = translationConfig;
  }

  @Bean(BeanNames.BEAN_CLIENT_DETAILS_SERVICE)
  public EuropeanaClientDetailsService getClientDetailsService() {
    EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
    clientDetailsService.setApiKeyServiceUrl(translationConfig.getApiKeyUrl());
    return clientDetailsService;
  }

  @Bean("requestMethodService")
  public ApiRequestPathMethodService getRequestPathMethodService() {
    return new ApiRequestPathMethodService();
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

  @Bean(name = ErrorConfig.BEAN_I18nService)
  public I18nService getI18nService() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasenames(ErrorConfig.COMMON_MESSAGE_SOURCE, "classpath:messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    return new I18nServiceImpl(messageSource);
  }

  @Bean(BeanNames.BEAN_SERVICE_PROVIDER)
  @DependsOn(BeanNames.BEAN_REDIS_MESSAGE_LISTENER_CONTAINER)
  public TranslationServiceProvider getTranslationServiceProvider() {
    if (StringUtils.isNotEmpty(serviceConfigFile)) {
      translationServiceProvider =
          new TranslationServiceProvider(translationConfig.getConfigFile(serviceConfigFile));
    } else {
      translationServiceProvider = new TranslationServiceProvider();
    }

    return translationServiceProvider;
  }

  @Bean(BeanNames.BEAN_RELATED_LANGUAGES)
  public RelatedLanguages getRelatedLanguages() throws AppConfigurationException {
    if (relatedLanguages != null) {
      return relatedLanguages;
    }

    Properties relatedLanugagesProps = null;
    if (StringUtils.isNotEmpty(relatedLanguagesConfigFile)) {
      File propertiesFile = translationConfig.getConfigFile(relatedLanguagesConfigFile);
      relatedLanugagesProps = loadPropertiesFromFile(propertiesFile);
    } else {
      relatedLanugagesProps =
          loadPropertiesFromClassPath(RelatedLanguages.RELATED_LANGUAGES_CONFIG_FILE);
    }

    relatedLanguages = new RelatedLanguages(relatedLanugagesProps);
    return relatedLanguages;
  }

  Properties loadPropertiesFromFile(@NonNull File propertiesFile) throws AppConfigurationException {

    Properties properties = new Properties();

    if (propertiesFile.exists()) {
      try (InputStream inputStream = Files.newInputStream(propertiesFile.toPath())) {
        properties.load(inputStream);
      } catch (IOException e) {
        // should actually not happen as the file exists
        throw new AppConfigurationException(
            "Unexpected error occured when reading properties from configFile: " + propertiesFile,
            e);
      }
    }

    return properties;
  }

  Properties loadPropertiesFromClassPath(@NonNull String configFileName)
      throws AppConfigurationException {

    Properties properties = new Properties();
    
    try (InputStream resourceAsStream =
        TranslationApiAutoconfig.class.getResourceAsStream(configFileName)) {
      
      if (resourceAsStream == null) {
        // file does not exist
        throw new AppConfigurationException(
            "Config file not found on the classpath: " + configFileName);
      }
      properties.load(resourceAsStream);
    } catch (IOException e) {
      // should actually not happen as the file exists
      throw new AppConfigurationException(
          "Unexpected error occured when reading properties from classpath: " + configFileName, e);
    }

    return properties;
  }

  @Bean(BeanNames.BEAN_LANGDETECT_PRE_PROCESSOR_SERVICE)
  public LangDetectionPreProcessor langDetectionPreProcessor() {
    return new LangDetectionPreProcessor(IsAlphabetic);
  }

  @Bean(BeanNames.BEAN_TRANSLATION_PRE_PROCESSOR_SERVICE)
  public TranslationPreProcessor translationPreProcessor() {
    return new TranslationPreProcessor(IsAlphabetic);
  }

  /*
   * Help, see connect to a standalone redis server:
   * https://medium.com/turkcell/making-first-connection-to-redis-with-java-
   * application-spring-boot- 4fc58e6fa173 A separate connection factory bean is needed here because
   * of the proper initialization, where some methods (e.g. afterPropertiesSet()) are called by
   * spring after the bean creation. Otherwise all these methods would need to be called manually
   * which is not the best solution.
   */
  @Bean(BeanNames.BEAN_REDIS_CONNECTION_FACTORY)
  LettuceConnectionFactory getRedisConnectionFactory() throws AppConfigurationException {
    // in case of integration tests, we do not need the SSL certificate
    LettuceClientConfiguration.LettuceClientConfigurationBuilder lettuceClientConfigurationBuilder =
        LettuceClientConfiguration.builder();
    // if redis secure protocol is used (rediss vs. redis)
    boolean sslEnabled = translationConfig.getRedisConnectionUrl().startsWith("rediss");
    if (sslEnabled) {
      final File truststore = getTrustoreFile();
      SslOptions sslOptions = SslOptions.builder().jdkSslProvider()
          .truststore(truststore, translationConfig.getTruststorePass()).build();

      ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();

      lettuceClientConfigurationBuilder.clientOptions(clientOptions).useSsl();
    }

    LettuceClientConfiguration lettuceClientConfiguration =
        lettuceClientConfigurationBuilder.build();

    RedisConfiguration redisConf = LettuceConnectionFactory
        .createRedisConfiguration(translationConfig.getRedisConnectionUrl());
    return new LettuceConnectionFactory(redisConf, lettuceClientConfiguration);
  }

  private File getTrustoreFile() throws AppConfigurationException {

    String truststorePathConfig = translationConfig.getTruststorePath();
    if (truststorePathConfig == null) {
      throw new AppConfigurationException(
          "A trustore must be provided in configurations when confinguring redis ssl connection");
    }
    // allow configurations to use the full path, for backward compatibility
    final File trustoreFile = translationConfig.getConfigFile(truststorePathConfig);
    if (!trustoreFile.exists()) {
      throw new AppConfigurationException(
          "Invalid config file location: " + trustoreFile.getAbsolutePath());
    }
    return trustoreFile;
  }

  @Bean(BeanNames.BEAN_REDIS_TEMPLATE)
  public RedisTemplate<String, CachedTranslation> getRedisTemplate(
      @Qualifier(BeanNames.BEAN_REDIS_CONNECTION_FACTORY) LettuceConnectionFactory redisConnectionFactory)
      throws AppConfigurationException {
    RedisTemplate<String, CachedTranslation> redisTemplate = new RedisTemplate<>();
    redisConnectionFactory.afterPropertiesSet();
    redisTemplate.setConnectionFactory(redisConnectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(
        new Jackson2JsonRedisSerializer<CachedTranslation>(CachedTranslation.class));
    redisTemplate.afterPropertiesSet();
    return redisTemplate;
  }

  @Bean(BeanNames.BEAN_REDIS_CACHE_SERVICE)
  @ConditionalOnProperty(name = "redis.connection.url")
  public RedisCacheService getRedisCacheService(
      @Qualifier(BeanNames.BEAN_REDIS_TEMPLATE) RedisTemplate<String, CachedTranslation> redisTemplate)
      throws AppConfigurationException {
    return new RedisCacheService(redisTemplate);
  }

  @Bean(BeanNames.BEAN_REDIS_MESSAGE_LISTENER_ADAPTER)
  MessageListenerAdapter listenerAdapter() {
    return new MessageListenerAdapter();
  }

  @Bean(BeanNames.BEAN_REDIS_MESSAGE_LISTENER_CONTAINER)
  RedisMessageListenerContainer getRedisMessageListenerContainer(
      @Qualifier(BeanNames.BEAN_REDIS_CONNECTION_FACTORY) LettuceConnectionFactory redisConnectionFactory,
      @Qualifier(BeanNames.BEAN_REDIS_MESSAGE_LISTENER_ADAPTER) MessageListenerAdapter messageListenerAdapter)
      throws AppConfigurationException {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    redisConnectionFactory.afterPropertiesSet();
    container.setConnectionFactory(redisConnectionFactory);
    /*
     * This is needed to avoid some cases redis closes all channels and does not allow any
     * subscriptions (please see here:
     * https://github.com/spring-projects/spring-data-redis/issues/2425). In this case we create one
     * channel that is never un-subscribed from.
     */
    container.addMessageListener(messageListenerAdapter, ChannelTopic.of("default"));
    return container;
  }

  @Override
  public void onApplicationEvent(ApplicationStartedEvent event) {
    // log beans for debuging purposes
    if (logger.isDebugEnabled()) {
      printRegisteredBeans(event.getApplicationContext());
    }

    // load either normal or dummy services (used for stress testing)
    loadServices(event);
  }

  private void loadServices(ApplicationStartedEvent event) {
    try {
      // verify required configurations for initialization of translation services
      verifyMandatoryProperties(event.getApplicationContext());

      // init translation services
      initTranslationServices(event.getApplicationContext());
    } catch (Exception e) {
      // gracefully stop the application in case of configuration problems (code 1
      // means exception occured at startup)
      logger.fatal(
          "Stopping application. Translation Service initialization failed due to configuration errors!",
          e);
      System.exit(SpringApplication.exit(event.getApplicationContext(), () -> 1));
    }
  }

  /**
   * Method for initialization of service provider using the service configurations
   * 
   * @param ctx the application context holding the initialized beans
   * @throws TranslationServiceConfigurationException if translations services cannot be correctly
   *         instantiated
   * @throws LangDetectionServiceConfigurationException if language detection services cannot be
   *         correctly instantiated
   */
  public void initTranslationServices(ApplicationContext ctx)
      throws TranslationServiceConfigurationException, LangDetectionServiceConfigurationException {
    translationServiceProvider.initTranslationServicesFromConfiguration();
  }

  /**
   * Method to verify required properties in translation config
   * 
   * @param ctx the application context holding references to instantiated beans
   */
  public void verifyMandatoryProperties(ApplicationContext ctx) {
    translationConfig.verifyRequiredProperties();
  }

  private void printRegisteredBeans(ApplicationContext ctx) {
    String[] beanNames = ctx.getBeanDefinitionNames();
    Arrays.sort(beanNames);
    logger.debug("Instantiated beans: {}", () -> StringUtils.join(beanNames, "\n"));
  }

}
