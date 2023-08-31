package eu.europeana.api.translation.config.services;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.SUPPORTED_LANGUAGES, TranslationAppConstants.SERVICES, TranslationAppConstants.DEFAULT_SERVICE_ID})
public class DetectCfg {

  private List<String> supported;
  private List<DetectServiceCfg> services;
  private String defaultServiceId;

  public DetectCfg() {
    super();
  }

  @JsonGetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public List<String> getSupported() {
    return supported;
  }

  @JsonSetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public void setSupported(List<String> supportedLangs) {
    this.supported = supportedLangs;
  }

  @JsonGetter(TranslationAppConstants.SERVICES)
  public List<DetectServiceCfg> getServices() {
    return services;
  }

  @JsonSetter(TranslationAppConstants.SERVICES)
  public void setServices(List<DetectServiceCfg> services) {
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
