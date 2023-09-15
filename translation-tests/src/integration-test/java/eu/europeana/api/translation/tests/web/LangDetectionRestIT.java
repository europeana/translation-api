package eu.europeana.api.translation.tests.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.google.cloud.translate.v3.TranslationServiceClient;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.GoogleLangDetectService;
import eu.europeana.api.translation.tests.BaseTranslationTest;
import eu.europeana.api.translation.tests.web.mock.MockGClient;
import eu.europeana.api.translation.tests.web.mock.MockGServiceStub;

@SpringBootTest
@AutoConfigureMockMvc
public class LangDetectionRestIT extends BaseTranslationTest {
 
  @Autowired TranslationConfig translationConfig;
  
  @Autowired GoogleLangDetectService googleLangDetectService;
  
  @BeforeAll
  void mockGoogleTranslate() throws IOException {
    TranslationServiceClient googleClient = new MockGClient(new MockGServiceStub());
    googleLangDetectService.init(googleClient);
  }
  
  @Test
  void langDetection() throws Exception {
    
    String requestJson = getJsonStringInput(LANG_DETECT_REQUEST);
    
    String result = mockMvc
        .perform(
            post(BASE_URL_DETECT)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    
    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    String langFieldValue = json.getString(TranslationAppConstants.LANG);
    assertNotNull(langFieldValue);
    List<String> langs = Collections.singletonList(json.getString(TranslationAppConstants.LANGS));
    assertTrue(langs.size()>0);
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }
  
  @Test
  void langDetectionGoogle() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_REQUEST_3);
    String result = mockMvc
        .perform(
            post(BASE_URL_DETECT)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    
    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    String langFieldValue = json.getString(TranslationAppConstants.LANG);
    assertNotNull(langFieldValue);    
    List<String> translations = Collections.singletonList(json.getString(TranslationAppConstants.TRANSLATIONS));
    assertTrue(translations.size()>0);
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);    
  }
  

  @Test
  void langDetectionWithoutLangParam() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_REQUEST_2);
    mockMvc
        .perform(
            post(BASE_URL_DETECT)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk());
  }

  @Test
  void langDetectionMissingTextParam() throws Exception {
    String requestJson = "{}";
    mockMvc
        .perform(
            post(BASE_URL_DETECT)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void langDetectionInvalidLangParam() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_BAD_REQUEST_1);
    mockMvc
        .perform(
            post(BASE_URL_DETECT)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void langDetectionInvalidServiceParam() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_BAD_REQUEST_2);
    mockMvc
        .perform(
            post(BASE_URL_DETECT)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isBadRequest());
  }  
}
