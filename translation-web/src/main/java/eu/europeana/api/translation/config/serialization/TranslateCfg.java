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
@JsonPropertyOrder({TranslationAppConstants.SERVICES, TranslationAppConstants.DEFAULT_CLASSNAME, TranslationAppConstants.SUPPORTED_LANGUAGES})
public class TranslateCfg {

  private List<TranslationServiceCfg> services;
  private String defaultClassname;
  private List<String> supportedLanguages;

  public TranslateCfg() {
    super();
  }

  @JsonGetter(TranslationAppConstants.DEFAULT_CLASSNAME)
  public String getDefaultClassname() {
    return defaultClassname;
  }

  @JsonSetter(TranslationAppConstants.DEFAULT_CLASSNAME)
  public void setDefaultClassname(String defaultClassname) {
    this.defaultClassname = defaultClassname;
  }

  @JsonGetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public List<String> getSupportedLanguages() {
    return supportedLanguages;
  }

  @JsonSetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public void setSupportedLanguages(List<String> supportedLanguages) {
    this.supportedLanguages = supportedLanguages;
  }

  @JsonGetter(TranslationAppConstants.SERVICES)
  public List<TranslationServiceCfg> getServices() {
    return services;
  }

  @JsonSetter(TranslationAppConstants.SERVICES)
  public void setServices(List<TranslationServiceCfg> services) {
    this.services = services;
  }

}
