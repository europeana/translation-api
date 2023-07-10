package eu.europeana.api.translation.config.serialization;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LangDetectResponseJsonConfig {

  private List<String> langs;
  private String lang;

  public LangDetectResponseJsonConfig() {
    super();
  }

  @JsonGetter(TranslationAppConstants.LANGS)
  public List<String> getLangs() {
    return langs;
  }

  public void setLangs(List<String> langs) {
    this.langs = langs;
  }

  @JsonGetter(TranslationAppConstants.LANG)
  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }
}
