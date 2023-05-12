package eu.europeana.api.translation.definitions.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslAppConstants.DETECT_ENDPOINT, TranslAppConstants.TRANSLATE_ENDPOINT})
public class TranslGlobalJsonConfig {

  private LangDetectJsonConfig langDetectConfig;
  private TranslJsonConfig translConfig;

  public TranslGlobalJsonConfig() {
    super();
  }

  @JsonGetter(TranslAppConstants.DETECT_ENDPOINT)
  public LangDetectJsonConfig getLangDetectConfig() {
    return langDetectConfig;
  }

  @JsonSetter(TranslAppConstants.DETECT_ENDPOINT)
  public void setLangDetectConfig(LangDetectJsonConfig langDetectConfig) {
    this.langDetectConfig = langDetectConfig;
  }

  @JsonGetter(TranslAppConstants.TRANSLATE_ENDPOINT)
  public TranslJsonConfig getTranslConfig() {
    return translConfig;
  }

  @JsonSetter(TranslAppConstants.TRANSLATE_ENDPOINT)
  public void setTranslConfig(TranslJsonConfig translConfig) {
    this.translConfig = translConfig;
  }
}
