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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.google.cloud.translate.v3.TranslationServiceClient;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.service.GoogleTranslationService;
import eu.europeana.api.translation.tests.BaseTranslationTest;
import eu.europeana.api.translation.tests.web.mock.MockGClient;
import eu.europeana.api.translation.tests.web.mock.MockGServiceStub;

@SpringBootTest
@AutoConfigureMockMvc
public class TranslationRestIT extends BaseTranslationTest {
 
  @Autowired TranslationConfig translationConfig;
  @Autowired GoogleTranslationService googleTranslationService;
  
  @BeforeAll
  private void mockGoogleTranslate() throws IOException {
    TranslationServiceClient googleClient = new MockGClient(new MockGServiceStub());
    googleTranslationService.init(googleClient);
    
  }
  
  @Test
  public void translationGoogle() throws Exception {
    String requestJson = getJsonStringInput(TRANSLATION_REQUEST);
    String result = mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    
    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    String langFieldValue = json.getString("lang");
    assertNotNull(langFieldValue);    
    List<String> translations = Collections.singletonList(json.getString("translations"));
    assertTrue(translations.size()>0);
    System.out.println("success!");
  }

  @Test
  public void translationPangeanic() throws Exception {
    String requestJson = getJsonStringInput(TRANSLATION_REQUEST_2);
    String result = mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    
    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    String langFieldValue = json.getString("lang");
    assertNotNull(langFieldValue);    
    List<String> translations = Collections.singletonList(json.getString("translations"));
    assertTrue(translations.size()>0);
    System.out.println("success!");
  }
  
  @Test
  public void translationWithServiceParam() throws Exception {
    String requestJson = getJsonStringInput(TRANSLATION_REQUEST_2);
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk());
  }

  @Test
  public void translationWithFallback() throws Exception {
    String requestJson = getJsonStringInput(TRANSLATION_WITH_FALLBACK);
    translationConfig.setTranslationGoogleProjectId("wrong-project-id");
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk());
  }

  @Test
  public void translateErrorNoTarget() throws Exception {
    String missingTarget = "{"
        + "\"source\": \"de\","
        + "\"detect\": false,"
        + "\"text\": [ \"eine Textzeile auf Deutsch\"]"
        + "}";
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(missingTarget))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Disabled("until specs are clarified")  
  public void translateWithDetect() throws Exception {
    String missingSource = "{"
        + "\"target\": \"en\","
        + "\"detect\": false,"
        + "\"text\": [ \"eine Textzeile auf Deutsch\"]"
        + "}";
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(missingSource))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void translateErrorMissingText() throws Exception {
    String missingText = "{"
        + "\"source\": \"de\","
        + "\"target\": \"en\","
        + "\"detect\": false"
        + "}";
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(missingText))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void translationInvalidSourceLangWithServiceParam() throws Exception {
    String requestJson = getJsonStringInput(TRANSLATION_BAD_REQUEST_1);
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void translationInvalidServiceParam() throws Exception {
    String requestJson = getJsonStringInput(TRANSLATION_BAD_REQUEST_2);
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isBadRequest());
  }
  
}
