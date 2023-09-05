package eu.europeana.api.translation.tests.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Collections;
import java.util.List;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.tests.BaseTranslationTest;

@Disabled
@SpringBootTest
@AutoConfigureMockMvc
public class LangDetectionRestIT extends BaseTranslationTest {
 
  @Autowired TranslationConfig translationConfig;
  
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
    String langFieldValue = json.getString("lang");
    assertNotNull(langFieldValue);
    List<String> langs = Collections.singletonList(json.getString("langs"));
    assertTrue(langs.size()>0);
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
