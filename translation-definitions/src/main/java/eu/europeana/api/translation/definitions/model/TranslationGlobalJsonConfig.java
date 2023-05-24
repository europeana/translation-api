package eu.europeana.api.translation.definitions.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.DETECT_ENDPOINT, TranslationAppConstants.TRANSLATE_ENDPOINT})
public class TranslationGlobalJsonConfig {

  private LangDetectJsonConfig langDetectConfig;
  private TranslationJsonConfig translConfig;

  public TranslationGlobalJsonConfig() {
    super();
  }

  @JsonGetter(TranslationAppConstants.DETECT_ENDPOINT)
  public LangDetectJsonConfig getLangDetectConfig() {
    return langDetectConfig;
  }

  @JsonSetter(TranslationAppConstants.DETECT_ENDPOINT)
  public void setLangDetectConfig(LangDetectJsonConfig langDetectConfig) {
    this.langDetectConfig = langDetectConfig;
  }

  @JsonGetter(TranslationAppConstants.TRANSLATE_ENDPOINT)
  public TranslationJsonConfig getTranslConfig() {
    return translConfig;
  }

  @JsonSetter(TranslationAppConstants.TRANSLATE_ENDPOINT)
  public void setTranslConfig(TranslationJsonConfig translConfig) {
    this.translConfig = translConfig;
  }
}
