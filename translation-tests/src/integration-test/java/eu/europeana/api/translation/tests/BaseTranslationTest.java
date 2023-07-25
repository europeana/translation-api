package eu.europeana.api.translation.tests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import eu.europeana.api.translation.TranslationApp;

@AutoConfigureMockMvc
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan(basePackageClasses = TranslationApp.class)
public abstract class BaseTranslationTest {

  protected static final String BASE_URL_TRANSLATE = "/translate";
  protected static final String BASE_URL_DETECT = "/detect";
  protected static final String LANG_DETECT_REQUEST = "/content/lang_detection_request.json";
  protected static final String LANG_DETECT_REQUEST_2 = "/content/lang_detection_request_2.json";
  protected static final String LANG_DETECT_BAD_REQUEST_1 = "/content/lang_detection_bad_request_1.json";
  protected static final String LANG_DETECT_BAD_REQUEST_2 = "/content/lang_detection_bad_request_2.json";
  protected static final String TRANSLATION_REQUEST = "/content/translation_request.json";
  protected static final String TRANSLATION_REQUEST_2 = "/content/translation_request_2.json";
  protected static final String TRANSLATION_WITH_FALLBACK = "/content/translation_with_fallback.json";
  protected static final String TRANSLATION_BAD_REQUEST_1 = "/content/translation_bad_request_1.json";
  protected static final String TRANSLATION_BAD_REQUEST_2 = "/content/translation_bad_request_2.json";
  
  protected MockMvc mockMvc;

  @Autowired
  protected WebApplicationContext wac;
  
  @BeforeAll
  protected void initApplication() {
    if (mockMvc == null) {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
  }

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("auth.read.enabled", () -> "false");
    registry.add("auth.write.enabled", () -> "false");
    // could be used to fix eclipse issues
    registry.add("scmBranch", () -> "dev");
    registry.add("buildNumber", () -> "99");
    registry.add("timestamp", () -> System.currentTimeMillis());
  }

  /**
   * This method extracts JSON content from a file
   * 
   * @param resource
   * @return JSON string
   * @throws IOException
   */
  protected String getJsonStringInput(String resource) throws IOException {
    try (InputStream resourceAsStream = getClass().getResourceAsStream(resource)) {
      List<String> lines = IOUtils.readLines(resourceAsStream, StandardCharsets.UTF_8);
      StringBuilder out = new StringBuilder();
      for (String line : lines) {
        out.append(line);
      }
      return out.toString();
    }
  }

}
