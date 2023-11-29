package eu.europeana.api.translation.tests.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
import eu.europeana.api.translation.definitions.model.TranslationObj;
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
  
  public static final String LANGUAGE_EN = "en";
  
  
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
    assertEquals(LANGUAGE_EN, langFieldValue);
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
    assertEquals(LANGUAGE_EN, langFieldValue);
        
    List<String> translations = Collections.singletonList(json.getString(TranslationAppConstants.TRANSLATIONS));
    assertTrue(translations.size()>0);
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }
  
  @Test
  void translationPangeanicNoSrcMultipleLanguages() throws Exception {
    String requestJson = getJsonStringInput(TRANSLATION_REQUEST_PANGEANIC_MULTIPLE_LANG);
    String result = mockMvc
        .perform(
            post(BASE_URL_TRANSLATE).characterEncoding(StandardCharsets.UTF_8)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isOk())
        .andReturn().getResponse()
        .getContentAsString();
    
    assertNotNull(result);
    JSONObject json = new JSONObject(result);
    String langFieldValue = json.getString(TranslationAppConstants.LANG);
    assertEquals(LANGUAGE_EN, langFieldValue);
        
    final JSONArray translations = json.optJSONArray(TranslationAppConstants.TRANSLATIONS);
    assertTrue(translations.length()==3);
    assertEquals("This is a dog", translations.getString(0));
    assertEquals("In the courtyard is played a puppy and a cat", translations.getString(1));
    //there is a MOCKMvc issue that doesn't deliver correct encoding, therefore we check only the end of the string
    assertTrue(translations.getString(2).endsWith("another cat"));
    
    String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
    assertNotNull(serviceFieldValue);
  }
  
  @Test
  void translationWithCaching() throws Exception {

    String requestJson = getJsonStringInput(TRANSLATION_REQUEST_CACHING);
    JSONObject reqJsonObj = new JSONObject(requestJson);
    JSONArray inputTexts = (JSONArray) reqJsonObj.get(TranslationAppConstants.TEXT);
    String sourceLang=reqJsonObj.getString(TranslationAppConstants.SOURCE_LANG);
    String targetLang=reqJsonObj.getString(TranslationAppConstants.TARGET_LANG);

    List<TranslationObj> translObjs = new ArrayList<TranslationObj>();
    for(int i=0;i<inputTexts.length();i++) {
      TranslationObj newTranslObj = new TranslationObj();
      newTranslObj.setSourceLang(sourceLang);
      newTranslObj.setTargetLang(targetLang);
      newTranslObj.setText((String) inputTexts.get(i));
      translObjs.add(newTranslObj);
    }    
    
    mockMvc
        .perform(
            post(BASE_URL_TRANSLATE)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .content(requestJson))
        .andExpect(status().isOk());
    
    //check that there are data in the cache
    redisCacheService.fillWithCachedTranslations(translObjs);
    assertTrue(translObjs.stream().filter(el -> el.getIsCached()).collect(Collectors.toList()).size()==2);
    
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
    Assertions.assertEquals(obj.get("code"), "mandatory_param_empty");
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
