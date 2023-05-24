package eu.europeana.api.translation.definitions.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.SOURCE_LANG, TranslationAppConstants.TARGET_LANG, TranslationAppConstants.CLASSNAME})
public class TranslationServiceJsonConfig {

  private String sourceLang;
  private String targetLang;
  private String classname;

  public TranslationServiceJsonConfig() {
    super();
  }

  @JsonGetter(TranslationAppConstants.SOURCE_LANG)
  public String getSourceLang() {
    return sourceLang;
  }

  @JsonSetter(TranslationAppConstants.SOURCE_LANG)
  public void setSourceLang(String sourceLang) {
    this.sourceLang = sourceLang;
  }

  @JsonGetter(TranslationAppConstants.TARGET_LANG)
  public String getTargetLang() {
    return targetLang;
  }

  @JsonSetter(TranslationAppConstants.TARGET_LANG)
  public void setTargetLang(String targetLang) {
    this.targetLang = targetLang;
  }

  @JsonGetter(TranslationAppConstants.CLASSNAME)
  public String getClassname() {
    return classname;
  }

  @JsonSetter(TranslationAppConstants.CLASSNAME)
  public void setClassname(String classname) {
    this.classname = classname;
  }

}
