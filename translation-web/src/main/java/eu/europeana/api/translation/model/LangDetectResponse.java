package eu.europeana.api.translation.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LangDetectResponse {

  private List<String> langs;
  private String lang;
  private String service;

  public LangDetectResponse() {
    super();
  }

  public LangDetectResponse(List<String> langs, String lang, String service) {
    this.langs = langs;
    this.lang = lang;
    this.service = service;
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

  @JsonGetter(TranslationAppConstants.SERVICE)
  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }
}
