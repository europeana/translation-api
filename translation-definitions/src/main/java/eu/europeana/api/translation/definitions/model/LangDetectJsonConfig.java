package eu.europeana.api.translation.definitions.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslAppConstants.DEFAULT_CLASSNAME, TranslAppConstants.SUPPORTED_LANGUAGES})
public class LangDetectJsonConfig {

  private String defaultClassname;
  private List<String> supportedLanguages;

  public LangDetectJsonConfig() {
    super();
  }

  @JsonGetter(TranslAppConstants.DEFAULT_CLASSNAME)
  public String getDefaultClassname() {
    return defaultClassname;
  }

  @JsonSetter(TranslAppConstants.DEFAULT_CLASSNAME)
  public void setDefaultClassname(String defaultClassname) {
    this.defaultClassname = defaultClassname;
  }

  @JsonGetter(TranslAppConstants.SUPPORTED_LANGUAGES)
  public List<String> getSupportedLanguages() {
    return supportedLanguages;
  }

  @JsonSetter(TranslAppConstants.SUPPORTED_LANGUAGES)
  public void setSupportedLanguages(List<String> supportedLanguages) {
    this.supportedLanguages = supportedLanguages;
  }

}
