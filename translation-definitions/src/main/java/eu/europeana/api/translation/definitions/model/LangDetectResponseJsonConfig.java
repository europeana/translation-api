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
public class LangDetectResponseJsonConfig {

  private List<String> langs;
  private String lang;

  public LangDetectResponseJsonConfig() {
    super();
  }

  @JsonGetter(TranslAppConstants.LANGS)
  public List<String> getLangs() {
    return langs;
  }

  public void setLangs(List<String> langs) {
    this.langs = langs;
  }

  @JsonGetter(TranslAppConstants.LANG)
  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }
}
