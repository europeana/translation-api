package eu.europeana.api.translation.tests.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.util.Optional;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.config.TranslationServiceProvider;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.google.GoogleLangDetectService;
import eu.europeana.api.translation.service.google.GoogleTranslationServiceClientWrapper;
import eu.europeana.api.translation.tests.BaseTranslationTest;

@SpringBootTest
@AutoConfigureMockMvc
public class LangDetectionRestIT extends BaseTranslationTest {

  @Autowired
  @Qualifier(BeanNames.BEAN_SERVICE_PROVIDER)
  private TranslationServiceProvider translationServiceProvider;

  @BeforeAll
  void mockGoogleDetect() throws IOException {
    // mock google language detection client
    GoogleTranslationServiceClientWrapper clientWrapper = mockGoogleClientWrapper();

    initGoogleService( BeanNames.SERVICE_GOOGLE_LANG_DETECT_SERVICE, clientWrapper);
    initGoogleService( BeanNames.SERVICE_GOOGLE_TRSH_LANG_DETECT_SERVICE, clientWrapper);

    LanguageDetectionService hybridLangDetectService = translationServiceProvider
        .getLangDetectionService(BeanNames.SERVICE_HYBRID_LANG_DETECT_SERVICE);

    // mock client in referenced google language detection service
    if (hybridLangDetectService != null) {
      Optional<LanguageDetectionService> serviceOptional =
          hybridLangDetectService.getReferencedServices().stream()
              .filter(s -> s.getClass().equals(GoogleLangDetectService.class)).findFirst();
      if (serviceOptional.isPresent()) {
        GoogleLangDetectService googleLangDetection =
            (GoogleLangDetectService) serviceOptional.get();
        googleLangDetection.init(clientWrapper);
      }
    }
  }



  private void initGoogleService( final String SERVICENAME, 
      GoogleTranslationServiceClientWrapper clientWrapper) {
    GoogleLangDetectService googleLangDetectService =
        (GoogleLangDetectService) translationServiceProvider
            .getLangDetectionService(SERVICENAME);
    if (googleLangDetectService != null) {
      googleLangDetectService.init(clientWrapper);
    }
  }



  @Test
  void langDetection() throws Exception {

    String requestJson = getJsonStringInput(LANG_DETECT_REQUEST);

    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertEquals(3, langs.length());
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }

  @Test
  void langDetectionApacheTika() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_APACHE_TIKA);
    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertTrue(langs.length() == 3 && "hr".equals(langs.getString(0))
        && "de".equals(langs.getString(1)) && "en".equals(langs.getString(2)));
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertEquals(BeanNames.SERVICE_TIKA_LANG_DETECT_SERVICE, serviceFieldValue);
  }
  
  @Test
  void langDetectionApacheTikaTrsh() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_APACHE_TIKA_TRSH);
    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertTrue(langs.length() == 3 && "hr".equals(langs.getString(0))
        && "de".equals(langs.getString(1)) && "en".equals(langs.getString(2)));
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertEquals(BeanNames.SERVICE_TIKA_TRSH_LANG_DETECT_SERVICE, serviceFieldValue);
  }

  @Test
  void langDetectionGoogle() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_REQUEST_3);
    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertEquals(2, langs.length());
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }

  @Test
  void langDetectionGoogleTrsh() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_REQUEST_GOOGLE_TRSH);
    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertEquals(2, langs.length());
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
    assertEquals(BeanNames.SERVICE_GOOGLE_TRSH_LANG_DETECT_SERVICE, serviceFieldValue);
  }


  @Test
  void langDetectionWithoutLangParam() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_REQUEST_2);
    mockMvc.perform(post(BASE_URL_DETECT)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).content(requestJson))
        .andExpect(status().isOk());
  }

  @Test
  void langDetectionMissingTextParam() throws Exception {
    String requestJson = "{}";
    String response = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .param("profile", "debug").content(requestJson))
        .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();

    JSONObject obj = new JSONObject(response);
    Assertions.assertEquals(obj.get("success"), false);
    Assertions.assertEquals(obj.get("status"), HttpStatus.BAD_REQUEST.value());
    Assertions.assertEquals(obj.get("code"), "mandatory_param_empty");
    Assertions.assertTrue(obj.has("error"));
    Assertions.assertTrue(obj.has("message"));
    Assertions.assertTrue(obj.has("timestamp"));
    Assertions.assertTrue(obj.has("path"));
    Assertions.assertTrue(obj.has("trace"));
  }

  @Test
  void langDetectionInvalidLangParam() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_BAD_REQUEST_1);
    mockMvc.perform(post(BASE_URL_DETECT)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void langDetectionInvalidServiceParam() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_BAD_REQUEST_2);
    mockMvc.perform(post(BASE_URL_DETECT)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).content(requestJson))
        .andExpect(status().isBadRequest());
  }

  // Hybrid lang detect tests

  @Test
  void langDetectionHybrid_1() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_HYBRID_REQUEST_1);
    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertEquals(3, langs.length());
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }

  @Test
  void langDetectionHybrid_2() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_HYBRID_REQUEST_2);
    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertEquals(2, langs.length());
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }

  @Test
  void langDetectionHybrid_InvalidHint() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_HYBRID_REQUEST_3);
    mockMvc.perform(post(BASE_URL_DETECT)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).content(requestJson))
        .andExpect(status().isBadRequest());
  }


  @Test
  void langDetectionHybrid_WhenNoLangDetected() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_HYBRID_REQUEST_4);
    String result = mockMvc
        .perform(post(BASE_URL_DETECT).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .content(requestJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertEquals(1, langs.length());
    assertTrue(langs.isNull(0));
  }
}
