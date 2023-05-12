package eu.europeana.api.translation.definitions.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslAppConstants.SOURCE_LANG, TranslAppConstants.TARGET_LANG, TranslAppConstants.CLASSNAME})
public class TranslServiceJsonConfig {

  private String sourceLang;
  private String targetLang;
  private String classname;

  public TranslServiceJsonConfig() {
    super();
  }

  @JsonGetter(TranslAppConstants.SOURCE_LANG)
  public String getSourceLang() {
    return sourceLang;
  }

  @JsonSetter(TranslAppConstants.SOURCE_LANG)
  public void setSourceLang(String sourceLang) {
    this.sourceLang = sourceLang;
  }

  @JsonGetter(TranslAppConstants.TARGET_LANG)
  public String getTargetLang() {
    return targetLang;
  }

  @JsonSetter(TranslAppConstants.TARGET_LANG)
  public void setTargetLang(String targetLang) {
    this.targetLang = targetLang;
  }

  @JsonGetter(TranslAppConstants.CLASSNAME)
  public String getClassname() {
    return classname;
  }

  @JsonSetter(TranslAppConstants.CLASSNAME)
  public void setClassname(String classname) {
    this.classname = classname;
  }

}
