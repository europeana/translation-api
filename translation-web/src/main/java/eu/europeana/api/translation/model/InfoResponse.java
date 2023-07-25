package eu.europeana.api.translation.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europeana.api.translation.config.serialization.TranslationServicesConfiguration;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InfoResponse {

  private ObjectNode build;
  private ObjectNode app;
  private TranslationServicesConfiguration config;

  public InfoResponse() {
    super();
  }

  @JsonGetter(TranslationAppConstants.BUILD_INFO)
  public ObjectNode getBuild() {
    return build;
  }

  public void setBuild(ObjectNode build) {
    this.build = build;
  }

  @JsonGetter(TranslationAppConstants.APP_INFO)
  public ObjectNode getApp() {
    return app;
  }

  public void setApp(ObjectNode app) {
    this.app = app;
  }

  @JsonGetter(TranslationAppConstants.CONFIG_INFO)
  public TranslationServicesConfiguration getConfig() {
    return config;
  }

  public void setConfig(TranslationServicesConfiguration config) {
    this.config = config;
  }

}
