package eu.europeana.api.translation.config;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Container for all settings that we load from the translation.properties file and optionally
 * override from translation.user.properties file
 */
@Configuration(BeanNames.BEAN_TRANSLATION_CONFIG)
@PropertySources({@PropertySource("classpath:translation.properties"),
@PropertySource(value = "translation.user.properties", ignoreResourceNotFound = true)})
public class TranslationConfig{

  public static final String CONFIG_FOLDER = "/opt/app/config"; 

  @Value("${europeana.apikey.jwttoken.signaturekey:}")
  private String apiKeyPublicKey;

  @Value("${authorization.api.name: translations}")
  private String authorizationApiName;

  @Value("${auth.read.enabled: true}")
  private boolean authReadEnabled;

  @Value("${auth.write.enabled: true}")
  private boolean authWriteEnabled;

  @Value("${europeana.apikey.serviceurl:}")
  private String apiKeyUrl;

  @Value("${translation.pangeanic.endpoint.detect}")
  private String pangeanicDetectEndpoint;

  @Value("${translation.pangeanic.endpoint.translate}")
  private String pangeanicTranslateEndpoint;
  
  @Value("${translation.google.projectId:}")
  private String googleTranslateProjectId;
  
  @Value("${translation.google.usehttpclient: false}")
  private boolean useGoogleHttpClient;
  
  @Value("${redis.connection.url:}")
  private String redisConnectionUrl;

  @Value("${truststore.path:}")
  private String truststorePath;

  @Value("${truststore.password:}")
  private String truststorePass;
  
  @Value("${translation.dummy.services:false}")
  private boolean useDummyServices;
  
  public TranslationConfig() {
    super();
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

  public String getApiKeyUrl() {
    return apiKeyUrl;
  }

  public String getPangeanicDetectEndpoint() {
    return pangeanicDetectEndpoint;
  }

  public String getPangeanicTranslateEndpoint() {
    return pangeanicTranslateEndpoint;
  }

  public String getGoogleTranslateProjectId() {
    return googleTranslateProjectId;
  }

  public void setTranslationGoogleProjectId(String googleTranslateProjectId) {
    this.googleTranslateProjectId = googleTranslateProjectId;
  }

  /** verify properties */
  public void verifyRequiredProperties() {
    List<String> missingProps = new ArrayList<>();

    if (isAuthReadEnabled() && StringUtils.isBlank(getApiKeyUrl())) {
      missingProps.add("europeana.apikey.jwttoken.signaturekey");
    }

    if (isAuthWriteEnabled() && StringUtils.isBlank(getApiKeyPublicKey())) {
      missingProps.add("europeana.apikey.serviceurl");
    }

    if (!missingProps.isEmpty()) {
      throw new IllegalStateException(String.format(
          "The following config properties are not set: %s", String.join("\n", missingProps)));
    }
  }

  public boolean useGoogleHttpClient() {
    return useGoogleHttpClient;
  }

  public String getRedisConnectionUrl() {
    return redisConnectionUrl;
  }

  public String getTruststorePath() {
    return truststorePath;
  }

  public String getTruststorePass() {
    return truststorePass;
  }

  public boolean isUseDummyServices() {
    return useDummyServices;
  }

  public String getConfigFolder() {
    return CONFIG_FOLDER;
  }
  
}
