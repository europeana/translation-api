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
public class TranslRequestJsonConfig {

  private String source;
  private String target;
  private Boolean detect;
  private List<String> text;

  public TranslRequestJsonConfig() {
    super();
  }

  public String getSource() {
    return source;
  }

  @JsonSetter(TranslAppConstants.SOURCE_LANG)
  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  @JsonSetter(TranslAppConstants.TARGET_LANG)
  public void setTarget(String target) {
    this.target = target;
  }

  public Boolean getDetect() {
    return detect;
  }

  @JsonSetter(TranslAppConstants.DETECT_BOOL)
  public void setDetect(Boolean detect) {
    this.detect = detect;
  }

  public List<String> getText() {
    return text;
  }

  @JsonSetter(TranslAppConstants.TEXT)
  public void setText(List<String> text) {
    this.text = text;
  }

}
