package eu.europeana.api.translation.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LangDetectRequest {

  private List<String> text;
  private String lang;

  public LangDetectRequest() {
    super();
  }

  public List<String> getText() {
    return text;
  }

  @JsonSetter(TranslationAppConstants.TEXT)
  public void setText(List<String> text) {
    this.text = text;
  }

  public String getLang() {
    return lang;
  }

  @JsonSetter(TranslationAppConstants.LANG)
  public void setLang(String lang) {
    this.lang = lang;
  }
}

