package eu.europeana.api.translation.config.serialization;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.SERVICE_ID, TranslationAppConstants.CLASSNAME, TranslationAppConstants.SUPPORTED_LANGUAGES})
public class DetectServiceCfg {

  private String id;
  private String classname;
  private List<String> supportedLangs;

  public DetectServiceCfg() {
    super();
  }

  @JsonGetter(TranslationAppConstants.SERVICE_ID)
  public String getId() {
    return id;
  }

  @JsonSetter(TranslationAppConstants.SERVICE_ID)
  public void setId(String id) {
    this.id = id;
  }

  @JsonGetter(TranslationAppConstants.CLASSNAME)
  public String getClassname() {
    return classname;
  }

  @JsonSetter(TranslationAppConstants.CLASSNAME)
  public void setClassname(String classname) {
    this.classname = classname;
  }

  @JsonGetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public List<String> getSupportedLangs() {
    return supportedLangs;
  }

  @JsonSetter(TranslationAppConstants.SUPPORTED_LANGUAGES)
  public void setSupportedLangs(List<String> supportedLangs) {
    this.supportedLangs = supportedLangs;
  }

}
