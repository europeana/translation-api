package eu.europeana.api.translation.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranslationRequest {

  private String source;
  private String target;
  private Boolean detect;
  private String service;
  private List<String> text;

  public TranslationRequest() {
    super();
  }

  public String getSource() {
    return source;
  }

  @JsonSetter(TranslationAppConstants.SOURCE_LANG)
  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  @JsonSetter(TranslationAppConstants.TARGET_LANG)
  public void setTarget(String target) {
    this.target = target;
  }

  public Boolean getDetect() {
    return detect;
  }

  @JsonSetter(TranslationAppConstants.DETECT_BOOL)
  public void setDetect(Boolean detect) {
    this.detect = detect;
  }

  public String getService() {
    return service;
  }

  @JsonSetter(TranslationAppConstants.SERVICE)
  public void setService(String service) {
    this.service = service;
  }

  public List<String> getText() {
    return text;
  }

  @JsonSetter(TranslationAppConstants.TEXT)
  public void setText(List<String> text) {
    this.text = text;
  }

}
