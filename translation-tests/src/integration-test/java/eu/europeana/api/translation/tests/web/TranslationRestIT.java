package eu.europeana.api.translation.tests.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
import eu.europeana.api.translation.service.google.GoogleTranslationService;
import eu.europeana.api.translation.service.google.GoogleTranslationServiceClientWrapper;
import eu.europeana.api.translation.tests.BaseTranslationTest;
import eu.europeana.api.translation.tests.web.mock.MockGClient;
import eu.europeana.api.translation.tests.web.mock.MockGServiceStub;
import eu.europeana.api.translation.web.service.RedisCacheService;
import redis.embedded.RedisServer;

@SpringBootTest
@AutoConfigureMockMvc
public class TranslationRestIT extends BaseTranslationTest {
 
  @Autowired TranslationConfig translationConfig;
  
  @Autowired GoogleTranslationService googleTranslationService;  
  
  @Autowired
  RedisCacheService redisCacheService;
  
  private static RedisServer redisServer = startRedisService();
  
  @Autowired 
  @Qualifier(BeanNames.BEAN_GOOGLE_TRANSLATION_CLIENT_WRAPPER)
  GoogleTranslationServiceClientWrapper clientWrapper;
  
  @BeforeAll
  void startMockServers() throws IOException {
    TranslationServiceClient googleClient = new MockGClient(new MockGServiceStub());
    clientWrapper.setClient(googleClient);
    googleTranslationService.init(clientWrapper);
  }

  static RedisServer startRedisService() {
    //start redis server
    RedisServer redisServer = new RedisServer(redisPort);
    redisServer.start();
    return redisServer;
  }
  
  @AfterAll void stopRedis() {
    if(redisServer != null) {
      redisServer.stop();
    }
  }
  
  
  @Test
  void translationGoogle() throws Exception {
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
    String langFieldValue = json.getString(TranslationAppConstants.LANG);
    assertNotNull(langFieldValue);    
    List<String> translations = Collections.singletonList(json.getString(TranslationAppConstants.TRANSLATIONS));
    assertTrue(translations.size()>0);
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);    
  }

  @Test
  void translationPangeanic() throws Exception {
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
    String langFieldValue = json.getString(TranslationAppConstants.LANG);
    assertNotNull(langFieldValue);    
    List<String> translations = Collections.singletonList(json.getString(TranslationAppConstants.TRANSLATIONS));
    assertTrue(translations.size()>0);
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }
  
  @Test
  void translationWithCaching() throws Exception {

    String requestJson = getJsonStringInput(TRANSLATION_REQUEST_CACHING);
    JSONObject reqJsonObj = new JSONObject(requestJson);
    JSONArray inputTexts = (JSONArray) reqJsonObj.get(TranslationAppConstants.TEXT);
    List<String> inputTextsList = new ArrayList<String>();
    for(int i=0;i<inputTexts.length();i++) {
      inputTextsList.add((String) inputTexts.get(i));
    }    
    String sourceLang=reqJsonObj.getString(TranslationAppConstants.SOURCE_LANG);
    String targetLang=reqJsonObj.getString(TranslationAppConstants.TARGET_LANG);
    
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk());
    
    //check that there are data in the cache
    List<String> redisContent = redisCacheService.getCachedTranslations(sourceLang, targetLang, inputTextsList);
    assertTrue(redisContent.size()==2 && Collections.frequency(redisContent, null)==0);
    
    String cachedResult = mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    
    assertNotNull(cachedResult);
    JSONObject json = new JSONObject(cachedResult);
    String langFieldValue = json.getString(TranslationAppConstants.LANG);
    assertNotNull(langFieldValue);    
    JSONArray translations = json.getJSONArray(TranslationAppConstants.TRANSLATIONS);
    assertTrue(translations.length()>0);
    
    redisCacheService.deleteAll();
  }

  @Test
  void translationWithServiceParam() throws Exception {
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
  void translationWithFallback() throws Exception {
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
  void translateErrorNoTarget() throws Exception {
    String missingTarget = "{"
        + "\"source\": \"de\","
        + "\"text\": [ \"eine Textzeile auf Deutsch\"]"
        + "}";
    String response = mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(missingTarget))
        .andExpect(status().isBadRequest())
        .andReturn().getResponse().getContentAsString();
    
    JSONObject obj = new JSONObject(response);
    Assertions.assertEquals(obj.get("success"), false);
    Assertions.assertEquals(obj.get("status"), HttpStatus.BAD_REQUEST.value());
    Assertions.assertTrue(obj.has("error"));
    Assertions.assertTrue(obj.has("message"));
    Assertions.assertTrue(obj.has("timestamp"));
    Assertions.assertTrue(obj.has("path"));
  }

  @Test
  @Disabled("until specs are clarified")  
  void translateWithDetect() throws Exception {
    String missingSource = "{"
        + "\"target\": \"en\","
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
  void translateErrorMissingText() throws Exception {
    String missingText = "{"
        + "\"source\": \"de\","
        + "\"target\": \"en\""
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
  void translationInvalidSourceLangWithServiceParam() throws Exception {
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
  void translationInvalidServiceParam() throws Exception {
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
