package eu.europeana.api.translation.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;

public abstract class IntegrationTestUtils {

  public static final String BASE_URL_TRANSLATE = "/translate";
  public static final String BASE_URL_DETECT = "/detect";
  
  public static final String LANG_DETECT_REQUEST = "/content/lang_detection_request.json";
  public static final String LANG_DETECT_APACHE_TIKA = "/content/lang_detection_apache_tika.json";
  public static final String LANG_DETECT_PANGEANIC_REQUEST = "/content/pangeanic/detect/lang_detection_pangeanic_request.json";
  public static final String LANG_DETECT_PANGEANIC_RESPONSE = "/content/pangeanic/detect/lang_detection_pangeanic_response.json";
  
  public static final String LANG_DETECT_REQUEST_2 = "/content/lang_detection_request_2.json";
  public static final String LANG_DETECT_PANGEANIC_REQUEST_2 = "/content/pangeanic/detect/lang_detection_pangeanic_request_2.json";
  public static final String LANG_DETECT_PANGEANIC_RESPONSE_2 = "/content/pangeanic/detect/lang_detection_pangeanic_response_2.json";

  public static final String LANG_DETECT_PANGEANIC_MULTIPLE_LANGUAGES_REQUEST = "/content/pangeanic/detect/lang_detection_pangeanic_translate_multiple_languages_request.json";
  public static final String LANG_DETECT_PANGEANIC_MULTIPLE_LANGUAGES_RESPONSE = "/content/pangeanic/detect/lang_detection_pangeanic_translate_multiple_languages_response.json";

  
  public static final String LANG_DETECT_REQUEST_3 = "/content/lang_detection_request_3.json";
  public static final String LANG_DETECT_GOOGLE_REQUEST = "/content/google/detect/lang_detect_google_request.txt";
  public static final String LANG_DETECT_GOOGLE_RESPONSE = "/content/google/detect/lang_detect_google_response.json";

  public static final String LANG_DETECT_BAD_REQUEST_1 = "/content/lang_detection_bad_request_1.json";
  public static final String LANG_DETECT_BAD_REQUEST_2 = "/content/lang_detection_bad_request_2.json";
  
  public static final String TRANSLATION_REQUEST = "/content/translation_request.json";
  public static final String TRANSLATION_GOOGLE_REQUEST = "/content/google/translate/translate_google_request.txt";
  public static final String TRANSLATION_GOOGLE_RESPONSE = "/content/google/translate/translate_google_response.json";
  
  public static final String TRANSLATION_GOOGLE_REQUEST_NO_SRC_LANG = "/content/google/translate/translate_google_request_no_src_lang.txt";
  public static final String TRANSLATION_GOOGLE_RESPONSE_NO_SRC_LANG = "/content/google/translate/translate_google_response_no_src_lang.json";
  
  
  public static final String TRANSLATION_REQUEST_2 = "/content/translation_request_2.json";
  public static final String TRANSLATION_PANGEANIC_REQUEST_2 = "/content/pangeanic/translate/translate_pangeanic_request_2.json";
  public static final String TRANSLATION_PANGEANIC_RESPONSE_2 = "/content/pangeanic/translate/translate_pangeanic_response_2.json";
  
  public static final String TRANSLATION_REQUEST_E_TRANSLATION = "/content/translation_request_eTranslation.json";
  
  public static final String TRANSLATION_REQUEST_PANGEANIC_MULTIPLE_LANG = "/content/translation_pangeanic_multiple_languages_request.json";
  public static final String TRANSLATION_PANGEANIC_REQUEST_MULTIPLE_LANG_DE = "/content/pangeanic/translate/translate_pangeanic_multiple_languages_request_DE.json";
  public static final String TRANSLATION_PANGEANIC_RESPONSE_MULTIPLE_LANG_DE = "/content/pangeanic/translate/translate_pangeanic_multiple_languages_response_DE.json";
  public static final String TRANSLATION_PANGEANIC_REQUEST_MULTIPLE_LANG_RO = "/content/pangeanic/translate/translate_pangeanic_multiple_languages_request_RO.json";
  public static final String TRANSLATION_PANGEANIC_RESPONSE_MULTIPLE_LANG_RO = "/content/pangeanic/translate/translate_pangeanic_multiple_languages_response_RO.json";
  
  
  public static final String TRANSLATION_REQUEST_CACHING = "/content/translation_request_caching.json";
  
  public static final String TRANSLATION_WITH_FALLBACK = "/content/translation_with_fallback.json";
  public static final String TRANSLATION_BAD_REQUEST_1 = "/content/translation_bad_request_1.json";
  public static final String TRANSLATION_BAD_REQUEST_2 = "/content/translation_bad_request_2.json";
  
  public static String loadFile(String resourcePath) throws IOException {
    return IOUtils.toString(
            Objects.requireNonNull(IntegrationTestUtils.class.getResourceAsStream(resourcePath)),
            StandardCharsets.UTF_8)
        .replace("\n", "");
  }

}
