package eu.europeana.api.translation.config;

import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.postpublication.translation.service.pangeanic.PangeanicV2LangDetectService;
import eu.europeana.postpublication.translation.service.pangeanic.PangeanicV2TranslationService;

@Configuration
public class TranslConfig {

  private static final Logger LOG = LogManager.getLogger(TranslConfig.class);

  @Autowired private TranslConfigProps translConfigProps;
    
  public TranslConfig() {
    LOG.info("Initializing translation configuration bean as: configuration");
  }
  
  @PostConstruct
  public void init() throws Exception {
    if (translConfigProps.isAuthReadEnabled() || translConfigProps.isAuthWriteEnabled()) {
      String jwtTokenSignatureKey = translConfigProps.getApiKeyPublicKey();
      if (jwtTokenSignatureKey == null || jwtTokenSignatureKey.isBlank()) {
        throw new IllegalStateException("The jwt token signature key cannot be null or empty.");
      }
    }
  }

  @Bean
  public EuropeanaClientDetailsService getClientDetailsService() {
    EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
    clientDetailsService.setApiKeyServiceUrl(translConfigProps.getApiKeyUrl());
    return clientDetailsService;
  }
  
  @Bean
  public PangeanicV2TranslationService translationService() {
      return new PangeanicV2TranslationService();
  }

  @Bean
  public PangeanicV2LangDetectService detectionService() {
      return new PangeanicV2LangDetectService();
  }

}
