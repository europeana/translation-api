package eu.europeana.api.translation.service.tika;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApacheTikaServiceConfiguration {

  private List<ThresholdConfiguration> hintThresholds;
  private List<ThresholdConfiguration> noHintThresholds;

  public ApacheTikaServiceConfiguration() {
    super();
  }

  @JsonGetter(ApacheTikaConstants.HINT_THRESHOLDS)
  public List<ThresholdConfiguration> getHintThresholds() {
    return hintThresholds;
  }

  @JsonSetter(ApacheTikaConstants.HINT_THRESHOLDS)
  public void setHintThresholds(List<ThresholdConfiguration> services) {
    this.hintThresholds = hintThresholds;
  }

  @JsonGetter(ApacheTikaConstants.NO_HINT_THRESHOLDS)
  public List<ThresholdConfiguration> getNoHintThresholds() {
    return noHintThresholds;
  }

  @JsonSetter(ApacheTikaConstants.NO_HINT_THRESHOLDS)
  public void setNoHintThresholds(List<ThresholdConfiguration> noHintThresholds) {
    this.noHintThresholds = noHintThresholds;
  }
}
