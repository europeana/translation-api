package eu.europeana.api.translation.definitions.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranslationResponseJsonConfig {

  private List<String> translations;
  private String lang;

  public TranslationResponseJsonConfig() {
    super();
  }

  @JsonGetter(TranslationAppConstants.TRANSLATIONS)
  public List<String> getTranslations() {
    return translations;
  }

  public void setTranslations(List<String> translations) {
    this.translations = translations;
  }

  @JsonGetter(TranslationAppConstants.LANG)
  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }
}
