package eu.europeana.api.translation.web.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Component;
import eu.europeana.api.commons.definitions.vocabulary.Role;
import eu.europeana.api.commons.nosql.service.ApiWriteLockService;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.commons.service.authorization.BaseAuthorizationService;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.web.auth.Roles;

@SuppressWarnings("deprecation")
@Component
public class TranslationAuthorizationService extends BaseAuthorizationService {

  protected final Logger logger = LogManager.getLogger(getClass());

  private final TranslationConfig translationConfig;
  private final EuropeanaClientDetailsService clientDetailsService;

  @Autowired
  public TranslationAuthorizationService(
      TranslationConfig translationConfig,
      EuropeanaClientDetailsService clientDetailsService) {
    this.translationConfig = translationConfig;
    this.clientDetailsService = clientDetailsService;
  }

  @Override
  protected ClientDetailsService getClientDetailsService() {
    return clientDetailsService;
  }

  @Override
  protected String getSignatureKey() {
    return translationConfig.getApiKeyPublicKey();
  }

  @Override
  protected String getApiName() {
    return translationConfig.getAuthorizationApiName();
  }

  @Override
  protected Role getRoleByName(String name) {
    return Roles.getRoleByName(name);
  }

  @Override
  protected ApiWriteLockService getApiWriteLockService() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  protected boolean isResourceAccessVerificationRequired(String operation) {
    return false;
  }

}
