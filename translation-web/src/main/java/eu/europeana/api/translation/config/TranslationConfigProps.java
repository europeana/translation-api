package eu.europeana.api.translation.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;

/**
 * Container for all settings that we load from the translation.properties file and optionally
 * override from translation.user.properties file
 */
@Configuration
@PropertySources({@PropertySource("classpath:translation.properties"),
    @PropertySource(value = "translation.user.properties", ignoreResourceNotFound = true)})
public class TranslationConfigProps implements InitializingBean {

  private static final Logger LOG = LogManager.getLogger(TranslationConfigProps.class);
  /** Matches spring.profiles.active property in test/resource application.properties file */
  public static final String ACTIVE_TEST_PROFILE = "test";

  @Value("${europeana.apikey.jwttoken.signaturekey:}")
//  @Value("${europeana.signaturekey:}")
  private String apiKeyPublicKey;

  @Value("${authorization.api.name: translation}")
  private String authorizationApiName;

  @Value("${auth.read.enabled: true}")
  private boolean authReadEnabled;

  @Value("${auth.write.enabled: true}")
  private boolean authWriteEnabled;

  @Value("${spring.profiles.active:}")
  private String activeProfileString;

  @Value("${europeana.apikey.serviceurl:}")
  private String apiKeyUrl;

  @Value("${translation.pangeanic.endpoint.detect}")
  private String pangeanicDetectEndpoint;

  @Value("${translation.pangeanic.endpoint.translate}")
  private String pangeanicTranslateEndpoint;

  public TranslationConfigProps() {
    LOG.info("Initializing TranslConfigProperties bean.");
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

    // if(StringUtils.isBlank(translConfigFile)) {
    // missingProps.add("translation.config.file");
    // }
    //
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

  @Bean
  public EuropeanaClientDetailsService getClientDetailsService() {
    EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
    clientDetailsService.setApiKeyServiceUrl(getApiKeyUrl());
    return clientDetailsService;
  }
}
