package eu.europeana.api.translation.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TranslationConfig {

  private static final Logger LOG = LogManager.getLogger(TranslationConfig.class);

  public TranslationConfig() {
    LOG.info("Initializing translation configuration bean as: configuration");
  }
  

  
}
