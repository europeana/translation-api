package eu.europeana.api.translation.config;

public interface BeanNames {

  String BEAN_PANGEANIC_TRANSLATION_SERVICE = "pangeanicTranslationService";
  String BEAN_GOOGLE_TRANSLATION_CLIENT_WRAPPER = "googleTranslationClientWrapper";
  String BEAN_GOOGLE_TRANSLATION_SERVICE = "googleTranslationService";
  String BEAN_E_TRANSLATION_SERVICE = "eTranslationService";
  String SERVICE_GOOGLE_LANG_DETECT_SERVICE = "GOOGLE";
  String SERVICE_GOOGLE_TRSH_LANG_DETECT_SERVICE = "GOOGLE_TRSH";
  String SERVICE_TIKA_LANG_DETECT_SERVICE = "TIKA";
  String SERVIC_TIKA_TRSH_LANG_DETECT_SERVICE = "TIKA_TRSH";
  String SERVICE_PANGEANIC_LANG_DETECT_SERVICE = "PANGEANIC";
  String SERVICE_HYBRID_LANG_DETECT_SERVICE = "HYBRID";
  String BEAN_DUMMY_TRANSLATION_SERVICE = "dummyTranslationService";
  String BEAN_DUMMY_LANG_DETECT_SERVICE = "dummyLangDetectService";

  String BEAN_I18N_SERVICE = "i18nService";
  String BEAN_CLIENT_DETAILS_SERVICE = "europeanaClientDetailsService";
  String BEAN_TRANSLATION_CONFIG = "translationConfig";
  String BEAN_SERVICE_PROVIDER = "translationServiceProvider";
  String BEAN_SERVICE_CONFIG_INFO_CONTRIBUTOR =
      "translationServiceConfigInfoContributor";
  String BEAN_REDIS_TEMPLATE = "redisTemplate";
  String BEAN_REDIS_CACHE_SERVICE = "redisCacheService";
  String BEAN_REDIS_MESSAGE_LISTENER_CONTAINER = "redisCacheMessageListenerContainer";
  String BEAN_REDIS_MESSAGE_LISTENER_ADAPTER = "redisMessageListenerAdapter";
  String BEAN_REDIS_CONNECTION_FACTORY = "redisConnectionFactory";
  String BEAN_TRANSLATION_PRE_PROCESSOR_SERVICE = "translationPreProcessorService";
  String BEAN_LANGDETECT_PRE_PROCESSOR_SERVICE = "langDetectPreProcessorService";
}
