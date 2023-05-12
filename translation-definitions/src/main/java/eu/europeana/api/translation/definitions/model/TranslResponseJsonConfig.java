package eu.europeana.api.translation.definitions.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.api.translation.definitions.vocabulary.TranslAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({})
public class TranslResponseJsonConfig {

  private List<String> translations;
  private String lang;

  public TranslResponseJsonConfig() {
    super();
  }

  @JsonGetter(TranslAppConstants.TRANSLATIONS)
  public List<String> getTranslations() {
    return translations;
  }

  public void setTranslations(List<String> translations) {
    this.translations = translations;
  }

  @JsonGetter(TranslAppConstants.LANG)
  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }
}
