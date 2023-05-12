package eu.europeana.api.translation.definitions.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({})
public class LangDetectRequestJsonConfig {

  private List<String> text;
  private String lang;

  public LangDetectRequestJsonConfig() {
    super();
  }

  public List<String> getText() {
    return text;
  }

  @JsonSetter(TranslAppConstants.TEXT)
  public void setText(List<String> text) {
    this.text = text;
  }

  public String getLang() {
    return lang;
  }

  @JsonSetter(TranslAppConstants.LANG)
  public void setLang(String lang) {
    this.lang = lang;
  }
}

