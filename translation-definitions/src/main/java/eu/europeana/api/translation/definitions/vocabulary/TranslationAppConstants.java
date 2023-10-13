package eu.europeana.api.translation.definitions.vocabulary;

public class TranslationAppConstants {

  //bean configuration constants
  public static final String BEAN_JSON_MAPPER = "translationJsonMapper";

  //api type constants
  public static final String DETECT_ENDPOINT = "detect";
  public static final String TRANSLATE_ENDPOINT = "translate";
  
  //app configuration fields
  public static final String SERVICE_ID = "id";
  public static final String DEFAULT_SERVICE_ID = "default";
  public static final String CLASSNAME = "classname";
  public static final String SUPPORTED_LANGUAGES = "supported";
  public static final String MAPPINGS = "mappings";
  public static final String SERVICES = "services";
  public static final String SERVICE = "service";
  public static final String FALLBACK = "fallback";
  public static final String SOURCE_LANG = "source";
  public static final String TARGET_LANG = "target";
  public static final char LANG_DELIMITER = '-';
  
  //api request/response fields
  public static final String TEXT = "text";
  public static final String LANGS = "langs";
  public static final String LANG = "lang";
  public static final String DETECT_BOOL = "detect";
  public static final String TRANSLATIONS = "translations";
  public static final String BUILD_INFO = "build";
  public static final String APP_INFO = "app";
  public static final String CONFIG_INFO = "config";

}
