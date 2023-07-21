package eu.europeana.api.translation.config.serialization;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.SERVICES, TranslationAppConstants.DEFAULT_SERVICE_ID})
public class TranslationCfg {

  private List<TranslationServiceCfg> services;
  private String defaultServiceId;

  public TranslationCfg() {
    super();
  }

  @JsonGetter(TranslationAppConstants.SERVICES)
  public List<TranslationServiceCfg> getServices() {
    return services;
  }

  @JsonSetter(TranslationAppConstants.SERVICES)
  public void setServices(List<TranslationServiceCfg> services) {
    this.services = services;
  }
  
  @JsonGetter(TranslationAppConstants.DEFAULT_SERVICE_ID)
  public String getDefaultServiceId() {
    return defaultServiceId;
  }

  @JsonSetter(TranslationAppConstants.DEFAULT_SERVICE_ID)
  public void setDefaultServiceId(String defaultServiceId) {
    this.defaultServiceId = defaultServiceId;
  }  

}
