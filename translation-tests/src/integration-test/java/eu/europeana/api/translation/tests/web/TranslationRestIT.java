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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.etranslation.ETranslationTranslationService;
import eu.europeana.api.translation.service.google.GoogleTranslationService;
import eu.europeana.api.translation.service.google.GoogleTranslationServiceClientWrapper;
import eu.europeana.api.translation.tests.BaseTranslationTest;
import eu.europeana.api.translation.web.service.RedisCacheService;

@SpringBootTest
@AutoConfigureMockMvc
public class TranslationRestIT extends BaseTranslationTest {
 
  @Autowired TranslationConfig translationConfig;
  
  @Autowired GoogleTranslationService googleTranslationService;  
  
  @Autowired
  RedisCacheService redisCacheService;
    
  public static final String LANGUAGE_EN = "en";
  
  
  @BeforeAll
  void startMockServers() throws IOException {
    GoogleTranslationServiceClientWrapper clientWrapper = mockGoogleClientWrapper();
    googleTranslationService.init(clientWrapper);
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

  class eTranslationSimulatorThreadForTextSnippetTranslation implements Runnable {
    private MockMvc mockMvc;
    public eTranslationSimulatorThreadForTextSnippetTranslation(MockMvc mockMvc) {
      this.mockMvc = mockMvc;
    }
    @Override
    public void run() {
      String result;
      try {
        String requestJson = getJsonStringInput(TRANSLATION_REQUEST_E_TRANSLATION);
        result = mockMvc
            .perform(
                post(BASE_URL_TRANSLATE)
                  .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .content(requestJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        
        JSONObject json = new JSONObject(result);
        String langFieldValue = json.getString(TranslationAppConstants.LANG);
        if(langFieldValue == null) {
          throw new IllegalArgumentException("langFieldValue must not be null");
        }
            
        List<String> translations = Collections.singletonList(json.getString(TranslationAppConstants.TRANSLATIONS));
        assertTrue(translations.contains("That is my dog.") && translations.contains("That is my tree."));
        String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
        if(serviceFieldValue == null) {
          throw new IllegalArgumentException("serviceFieldValue must not be null");
        }
      } catch (Exception e) {
        throw new RuntimeException("cannot run simulator for eTranslation callback!", e);
      }
    }
  }

  class eTranslationSimulatorThreadForDocumentTranslation implements Runnable {
    private MockMvc mockMvc;
    public eTranslationSimulatorThreadForDocumentTranslation(MockMvc mockMvc) {
      this.mockMvc = mockMvc;
    }
    @Override
    public void run() {
      try {
        String requestJson = getJsonStringInput(TRANSLATION_REQUEST_E_TRANSLATION_LONGER);
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
        assertTrue(translations.contains("test first line in German, eight on caching, no cache 1!") 
            && translations.contains("a second text in German, can be cached....") 
            && translations.contains("a second text in German, can be cached....")
            && translations.contains("a third text in German, such as this")
            && translations.contains("and a fourth text such as: today’s news on www.heute.at"));
        String serviceFieldValue = json.getString(TranslationAppConstants.SERVICE);
        assertNotNull(serviceFieldValue);
      } catch (Exception e) {
      }
    }
  }

  /*
   * eTranslation that uses a text snippet based translation
   */
  @Test
  void translationETranslationTextSnippet() throws Exception {
    Thread thread = new Thread(new eTranslationSimulatorThreadForTextSnippetTranslation(mockMvc));
    thread.start();
    Thread.sleep(1000);
    //trigger the eTranslation callback manually
    //computed in advance using the code in the eTransl service
    String eTranslRef="et:deenre7d+w";
    StringBuilder translatedText=new StringBuilder();
    translatedText.append("That is my dog.");
    translatedText.append(ETranslationTranslationService.MARKUP_DELIMITER);
    translatedText.append("That is my tree.");
    
    mockMvc
    .perform(
        post(ETranslationTranslationService.PATH_CALLBACK).characterEncoding(StandardCharsets.UTF_8)
        .param("external-reference", eTranslRef)
        .param("request-id", "1")
        .param("translated-text", translatedText.toString()))
    .andExpect(status().isOk());

    thread.join();
    
  }

  /*
   * eTranslation that uses a document based translation
   */
  @Test
  void translationETranslationDocument() throws Exception {    
    Thread thread = new Thread(new eTranslationSimulatorThreadForDocumentTranslation(mockMvc));
    thread.start();
    Thread.sleep(1000);
    //trigger the eTranslation callback manually
    //computed in advance using the code in the eTransl service
    String eTranslRef="et:deen9Aie9A";
    //base64 encoded translations
    String translatedText="dGVzdCBmaXJzdCBsaW5lIGluIEdlcm1hbiwgZWlnaHQgb24gY2FjaGluZywgbm8gY2FjaGUgMSEKW25vdHJhbnNsYXRlXWRlZW5QVnNhT2dbL25vdHJhbnNsYXRlXQphIHNlY29uZCB0ZXh0IGluIEdlcm1hbiwgY2FuIGJlIGNhY2hlZC4uLi4KW25vdHJhbnNsYXRlXWRlZW5QVnNhT2dbL25vdHJhbnNsYXRlXQphIHRoaXJkIHRleHQgaW4gR2VybWFuLCBzdWNoIGFzIHRoaXMKW25vdHJhbnNsYXRlXWRlZW5QVnNhT2dbL25vdHJhbnNsYXRlXQphbmQgYSBmb3VydGggdGV4dCBzdWNoIGFzOiB0b2RheeKAmXMgbmV3cyBvbiB3d3cuaGV1dGUuYXQ=";
    
    mockMvc
    .perform(
        post(ETranslationTranslationService.PATH_CALLBACK).characterEncoding(StandardCharsets.UTF_8)
        .param("external-reference", eTranslRef)
        .param("request-id", "1")
        .content(translatedText))
    .andExpect(status().isOk());

    thread.join();
   
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
    assertEquals(4, translations.length());
    assertEquals("This is a dog", translations.getString(0));
    assertEquals("In the courtyard is played a puppy and a cat", translations.getString(1));
    //there is a MOCKMvc issue that doesn't deliver correct encoding, therefore we check only the end of the string
    assertTrue(translations.getString(2).endsWith("another cat"));
    //translation is set to null for texts where the language detection returns null
    assertTrue(translations.isNull(3));
    
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
    final List<TranslationObj> cachedTranslations = translObjs.stream().filter(el -> el.isRetrievedFromCache()).toList();
    //check if all are availble in the cache
    assertTrue(cachedTranslations.size() == translObjs.size());
    
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
