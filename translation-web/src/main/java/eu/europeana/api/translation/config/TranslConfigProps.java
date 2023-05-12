package eu.europeana.api.translation.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Container for all settings that we load from the translation.properties file and optionally 
 * override from translation.user.properties file
 */
@Configuration
@PropertySources({
  @PropertySource("classpath:translation.properties"),
  @PropertySource(
      value = "classpath:translation.user.properties",
      ignoreResourceNotFound = true)
})
public class TranslConfigProps implements InitializingBean {

  private static final Logger LOG = LogManager.getLogger(TranslConfigProps.class);
  /** Matches spring.profiles.active property in test/resource application.properties file */
  public static final String ACTIVE_TEST_PROFILE = "test";
  
  @Value("${europeana.apikey.jwttoken.signaturekey}")
  private String apiKeyPublicKey;

  @Value("${authorization.api.name}")
  private String authorizationApiName;

  @Value("${auth.read.enabled: true}")
  private boolean authReadEnabled;

  @Value("${auth.write.enabled: true}")
  private boolean authWriteEnabled;

  @Value("${spring.profiles.active:}")
  private String activeProfileString;

  @Value("${translation.config.file}")
  private String translConfigFile;
  
  @Value("${europeana.apikey.serviceurl}")
  private String apiKeyUrl;

  public TranslConfigProps() {
    LOG.info("Initializing TranslConfigProperties bean as: configuration");
  }

  public String getApiKeyPublicKey() {
    return apiKeyPublicKey;
  }

  public String getAuthorizationApiName() {
    return authorizationApiName;
  }

  public boolean isAuthReadEnabled() {
    return authReadEnabled;
  }

  public boolean isAuthWriteEnabled() {
    return authWriteEnabled;
  }
  
  public String getTranslConfigFile() {
    return translConfigFile;
  }  
  
  public String getApiKeyUrl() {
    return apiKeyUrl;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (testProfileNotActive(activeProfileString)) {
      verifyRequiredProperties();
    }
  }

  public static boolean testProfileNotActive(String activeProfileString) {
    return Arrays.stream(activeProfileString.split(",")).noneMatch(ACTIVE_TEST_PROFILE::equals);
  }

  /** verify properties */
  private void verifyRequiredProperties() {
    List<String> missingProps = new ArrayList<>();

    if(StringUtils.isBlank(translConfigFile)) {
      missingProps.add("translation.config.file");      
    }
    
    if (!missingProps.isEmpty()) {
      throw new IllegalStateException(
          String.format(
              "The following config properties are not set: %s", String.join("\n", missingProps)));
    }
  }
  
}
