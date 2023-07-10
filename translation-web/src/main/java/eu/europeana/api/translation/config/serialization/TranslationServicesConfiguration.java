package eu.europeana.api.translation.config.serialization;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.DETECT_ENDPOINT, TranslationAppConstants.TRANSLATE_ENDPOINT})
public class TranslationServicesConfiguration {

  private DetectCfg langDetectConfig;
  private TranslateCfg translConfig;

  public TranslationServicesConfiguration() {
    super();
  }

  @JsonGetter(TranslationAppConstants.DETECT_ENDPOINT)
  public DetectCfg getLangDetectConfig() {
    return langDetectConfig;
  }

  @JsonSetter(TranslationAppConstants.DETECT_ENDPOINT)
  public void setLangDetectConfig(DetectCfg langDetectConfig) {
    this.langDetectConfig = langDetectConfig;
  }

  @JsonGetter(TranslationAppConstants.TRANSLATE_ENDPOINT)
  public TranslateCfg getTranslConfig() {
    return translConfig;
  }

  @JsonSetter(TranslationAppConstants.TRANSLATE_ENDPOINT)
  public void setTranslConfig(TranslateCfg translConfig) {
    this.translConfig = translConfig;
  }
}
