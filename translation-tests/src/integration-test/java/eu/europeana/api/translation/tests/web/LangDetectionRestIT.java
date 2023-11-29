package eu.europeana.api.translation.tests.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
import com.google.cloud.translate.v3.TranslationServiceClient;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.google.GoogleLangDetectService;
import eu.europeana.api.translation.service.google.GoogleTranslationServiceClientWrapper;
import eu.europeana.api.translation.tests.BaseTranslationTest;
import eu.europeana.api.translation.tests.web.mock.MockGClient;
import eu.europeana.api.translation.tests.web.mock.MockGServiceStub;

@SpringBootTest
@AutoConfigureMockMvc
public class LangDetectionRestIT extends BaseTranslationTest {
 
  @Autowired TranslationConfig translationConfig;
  
  @Autowired GoogleLangDetectService googleLangDetectService;
  
  @Autowired 
  @Qualifier(BeanNames.BEAN_GOOGLE_TRANSLATION_CLIENT_WRAPPER)
  GoogleTranslationServiceClientWrapper clientWrapper;
  
  @BeforeAll
  void mockGoogleDetect() throws IOException {
    TranslationServiceClient googleClient = new MockGClient(new MockGServiceStub());
    clientWrapper.setClient(googleClient);
    googleLangDetectService.init(clientWrapper);
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
    List<String> langs = Collections.singletonList(json.getString(TranslationAppConstants.LANGS));
    assertTrue(langs.size()>0);
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }

  @Test
  void langDetectionApacheTika() throws Exception {
    String requestJson = getJsonStringInput(LANG_DETECT_APACHE_TIKA);
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
    JSONArray langs = json.getJSONArray(TranslationAppConstants.LANGS);
    assertTrue(langs.length()==3 && "hr".equals(langs.getString(0)) && "de".equals(langs.getString(1)) && "en".equals(langs.getString(2)));
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertTrue("APACHE-TIKA".equals(serviceFieldValue));
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
    List<String> langs = Collections.singletonList(json.getString(TranslationAppConstants.LANGS));
    assertTrue(langs.size()>0);
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
    String response = mockMvc
        .perform(
            post(BASE_URL_DETECT)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .param("profile", "debug")
              .content(requestJson))
        .andExpect(status().isBadRequest())
        .andReturn().getResponse().getContentAsString();
    
    JSONObject obj = new JSONObject(response);
    Assertions.assertEquals(obj.get("success"), false);
    Assertions.assertEquals(obj.get("status"), HttpStatus.BAD_REQUEST.value());
    Assertions.assertTrue(obj.has("error"));
    Assertions.assertTrue(obj.has("message"));
    Assertions.assertTrue(obj.has("timestamp"));
    Assertions.assertTrue(obj.has("path"));
    Assertions.assertTrue(obj.has("trace"));
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
