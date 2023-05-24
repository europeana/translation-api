package eu.europeana.api.translation.web.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Component;
import eu.europeana.api.commons.definitions.vocabulary.Role;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.commons.service.authorization.BaseAuthorizationService;
import eu.europeana.api.translation.config.TranslationConfigProps;
import eu.europeana.api.translation.web.auth.Roles;

@Component
public class TranslationAuthorizationService extends BaseAuthorizationService {

  protected final Logger logger = LogManager.getLogger(getClass());

  private final TranslationConfigProps translConfiguration;
  private final EuropeanaClientDetailsService clientDetailsService;

  @Autowired
  public TranslationAuthorizationService(
      TranslationConfigProps translConfiguration,
      EuropeanaClientDetailsService clientDetailsService) {
    this.translConfiguration = translConfiguration;
    this.clientDetailsService = clientDetailsService;
  }

  @Override
  protected ClientDetailsService getClientDetailsService() {
    return clientDetailsService;
  }

  @Override
  protected String getSignatureKey() {
    return translConfiguration.getApiKeyPublicKey();
  }

  @Override
  protected String getApiName() {
    return translConfiguration.getAuthorizationApiName();
  }

  @Override
  protected Role getRoleByName(String name) {
    return Roles.getRoleByName(name);
  }

}
