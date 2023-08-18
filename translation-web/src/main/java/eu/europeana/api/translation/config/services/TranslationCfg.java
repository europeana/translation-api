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
@JsonPropertyOrder({TranslationAppConstants.SUPPORTED_LANGUAGES, TranslationAppConstants.SERVICES, TranslationAppConstants.MAPPINGS, TranslationAppConstants.DEFAULT_SERVICE_ID})
public class TranslationCfg {

  private List<TranslationLangPairCfg> supported;
  private List<TranslationServiceCfg> services;
  private List<TranslationMappingCfg> mappings;
  private String defaultServiceId;

  public TranslationCfg() {
    super();
  }

  @JsonGetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public List<TranslationLangPairCfg> getSupported() {
    return supported;
  }

  @JsonSetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public void setSupported(List<TranslationLangPairCfg> supported) {
    this.supported = supported;
  }

  @JsonGetter(TranslationAppConstants.SERVICES)
  public List<TranslationServiceCfg> getServices() {
    return services;
  }

  @JsonSetter(TranslationAppConstants.SERVICES)
  public void setServices(List<TranslationServiceCfg> services) {
    this.services = services;
  }
  
  @JsonGetter(TranslationAppConstants.MAPPINGS)
  public List<TranslationMappingCfg> getMappings() {
    return mappings;
  }

  @JsonSetter(TranslationAppConstants.MAPPINGS)
  public void setMappings(List<TranslationMappingCfg> mappings) {
    this.mappings = mappings;
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
