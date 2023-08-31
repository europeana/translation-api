package eu.europeana.api.translation.tests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
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
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@AutoConfigureMockMvc
@DirtiesContext
@ComponentScan(basePackageClasses = TranslationApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTranslationTest extends IntegrationTestUtils {

  protected MockMvc mockMvc;
  protected static final Logger LOG = LogManager.getLogger(BaseTranslationTest.class);
  

  /** Maps Metis dereferenciation URIs to mocked XML responses */
  public static final Map<String, String> LANG_DETECT_RESPONSE_MAP = initLanguageDetectMap();
  public static final Map<String, String> TRANSLATION_RESPONSE_MAP = initTranslationMap();
  
  /** MockWebServer needs to be static, so we can inject its port into the Spring context. */
  private static MockWebServer mockPangeanic = startPangeanicMockServer();
  
  private static Map<String, String> initLanguageDetectMap() {
    try {
      return Map.of(
          loadFile(LANG_DETECT_PANGEANIC_REQUEST).trim(), loadFile(LANG_DETECT_PANGEANIC_RESPONSE),
          loadFile(LANG_DETECT_PANGEANIC_REQUEST_2).trim(), loadFile(LANG_DETECT_PANGEANIC_RESPONSE_2)
      );
    } catch (IOException e) {
      throw new RuntimeException("Test initialization exception!", e);
    }
  }

  private static Map<String, String> initTranslationMap() {
    try {
      return Map.of(
          loadFile(TRANSLATION_PANGEANIC_REQUEST_2).trim(), loadFile(TRANSLATION_PANGEANIC_RESPONSE_2)
      );
    } catch (IOException e) {
      throw new RuntimeException("Test initialization exception!", e);
    }
  }
  
  private static MockWebServer startPangeanicMockServer() {
    MockWebServer mockPangeanic = new MockWebServer();
    mockPangeanic.setDispatcher(setupPangeanicDispatcher());
    try {
      mockPangeanic.start();
    } catch (IOException e) {
      throw new RuntimeException("Cannot start pangeanic mock server", e);
    }
    return mockPangeanic;
  }
  
  @Autowired
  protected WebApplicationContext wac;

  @BeforeAll
  private void initServices() {
    if (mockMvc == null) {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
      TranslationApp.initTranslationServices(wac);
      }
  }

  @AfterAll
  private void stopServices() {
    //cannot stop the mock server here as all test classes are run by the same runner and the server is static variable
    //only  
  }
  
  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("auth.read.enabled", () -> "false");
    registry.add("auth.write.enabled", () -> "false");
    // could be used to fix eclipse issues
    registry.add("scmBranch", () -> "dev");
    registry.add("buildNumber", () -> "99");
    registry.add("timestamp", () -> System.currentTimeMillis());

    registry.add("translation.pangeanic.endpoint.detect",
        () -> {
          final String pangeanicMockDetect = String.format("http://%s:%s/pangeanic/detect", mockPangeanic.getHostName(),
              mockPangeanic.getPort());
          LOG.info("Detect endpoint: {}", pangeanicMockDetect);
          return pangeanicMockDetect;
        });
    registry.add("translation.pangeanic.endpoint.translate",
        () -> {
          final String pangeanicMockTranslate = String.format("http://%s:%s/pangeanic/translate", mockPangeanic.getHostName(),
              mockPangeanic.getPort());
          LOG.info("Translate endpoint: {}", pangeanicMockTranslate);
          return pangeanicMockTranslate;
        });

    registry.add("translation.google.projectId", () -> "google-test");
    registry.add("translation.google.usehttpclient", () -> "true");
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

  private static Dispatcher setupPangeanicDispatcher() {
    return new Dispatcher() {
      @NotNull
      @Override
      public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
        try {
          
          String requestBody = Objects.requireNonNull(request.getBody().readUtf8());
          boolean isTranslationRequest = request.getPath().endsWith(BASE_URL_TRANSLATE);
          boolean isLangDetectionRequest = request.getPath().endsWith(BASE_URL_DETECT);
          String responseBody = "";
          if(isTranslationRequest) {
            responseBody = TRANSLATION_RESPONSE_MAP.getOrDefault(requestBody.trim(), "");
          }
          
          if(isLangDetectionRequest) {
            responseBody = LANG_DETECT_RESPONSE_MAP.getOrDefault(requestBody.trim(), "");
          }
          
          return new MockResponse().setResponseCode(200).setBody(responseBody);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }
}
