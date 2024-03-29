package eu.europeana.api.translation.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.config.TranslationServiceProvider;

@Component(BeanNames.BEAN_SERVICE_CONFIG_INFO_CONTRIBUTOR)
public class TranslationConfigInfoContributor implements InfoContributor{

  @Autowired
  @Qualifier(BeanNames.BEAN_SERVICE_PROVIDER)
  TranslationServiceProvider translationServiceProvider;
  
  @Override
  public void contribute(Builder builder) {
    builder.withDetail("config", translationServiceProvider.getTranslationServicesConfig());
  }

}
