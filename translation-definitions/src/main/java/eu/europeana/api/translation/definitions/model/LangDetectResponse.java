package eu.europeana.api.translation.definitions.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LangDetectResponse {

  private List<String> langs;
  private String service;

  public LangDetectResponse() {
    super();
  }

  /**
   * Constructor with object initialization
   * @param langs detected languages
   * @param service the service used to detect the languages
   */
  public LangDetectResponse(List<String> langs, String service) {
    this.langs = langs;
    this.service = service;
  }
  
  
  @JsonGetter(TranslationAppConstants.LANGS)
  public List<String> getLangs() {
    return langs;
  }

  public void setLangs(List<String> langs) {
    this.langs = langs;
  }

  @JsonGetter(TranslationAppConstants.SERVICE)
  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }
}
