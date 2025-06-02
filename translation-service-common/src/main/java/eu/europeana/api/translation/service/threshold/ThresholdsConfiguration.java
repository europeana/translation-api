package eu.europeana.api.translation.service.threshold;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;

/**
 * Configuration of confidence thresholds based on text length
 * 
 * @author Nuno Freire
 * @since 04/03/2025
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdsConfiguration {
  protected static final Logger LOG = LogManager.getLogger(ThresholdsConfiguration.class);

  private Float nonSupportedLangPrior;
  private List<ThresholdRangeConfiguration> hintThresholds;
  private List<ThresholdRangeConfiguration> noHintThresholds;

  /**
   * Default constructor
   */
  public ThresholdsConfiguration() {
    super();
  }

  @JsonGetter(ThresholdsConstants.HINT_THRESHOLDS)
  public List<ThresholdRangeConfiguration> getHintThresholds() {
    return hintThresholds;
  }

  @JsonSetter(ThresholdsConstants.HINT_THRESHOLDS)
  public void setHintThresholds(List<ThresholdRangeConfiguration> hintThresholds) {
    this.hintThresholds = hintThresholds;
  }

  @JsonGetter(ThresholdsConstants.NO_HINT_THRESHOLDS)
  public List<ThresholdRangeConfiguration> getNoHintThresholds() {
    return noHintThresholds;
  }

  @JsonSetter(ThresholdsConstants.NO_HINT_THRESHOLDS)
  public void setNoHintThresholds(List<ThresholdRangeConfiguration> noHintThresholds) {
    this.noHintThresholds = noHintThresholds;
  }

  @JsonGetter(ThresholdsConstants.NON_SUPPORTED_LANG_PRIOR)
  public Float getNonSupportedLangPrior() {
    return nonSupportedLangPrior;
  }

  @JsonSetter(ThresholdsConstants.NON_SUPPORTED_LANG_PRIOR)
  public void setNonSupportedLangPrior(float nonSupportedLangPrior) {
    this.nonSupportedLangPrior = nonSupportedLangPrior;
  }

  public void validateThresholds() throws LangDetectionServiceConfigurationException {
    validateThresholdRanges(hintThresholds);
    validateThresholdRanges(noHintThresholds);
  }
  
  private void validateThresholdRanges(List<ThresholdRangeConfiguration> confidenceThresholds)
      throws LangDetectionServiceConfigurationException {
    ThresholdRangeConfiguration previous = null;
    for (ThresholdRangeConfiguration threshold : confidenceThresholds) {
      if (threshold.getMinLength() == null)
        throw new LangDetectionServiceConfigurationException("Minimum length is missing");
      if (previous == null) {
        if (threshold.getMinLength() != 0)
          throw new LangDetectionServiceConfigurationException("First threshold does not start at zero");
      } else {
        if (previous.getMaxLength() == null || (threshold.getMinLength() != (previous.getMaxLength() + 1)))
          throw new LangDetectionServiceConfigurationException("Gap or overlap detected in threshold lengths");
      }
      if (threshold.getConfidenceThreshold() != null
          && (threshold.getConfidenceThreshold() < 0 || threshold.getConfidenceThreshold() > 1))
        throw new LangDetectionServiceConfigurationException(
            "confidence threshold must be a number between 0 and 1 (both inclusive)");
      previous = threshold;
    }
    if (previous == null)
      throw new LangDetectionServiceConfigurationException("No threshold configured");
    if (previous.getMaxLength() != null)
      throw new LangDetectionServiceConfigurationException("The last threshold should be unbounded");
  }


  /**
   * Checks if a detection is acceptable 
   * 
   * @param sourceText the text on which the detection was executed
   * @param langHint the language hint
   * @param confidence the confidence score obtained
   * @return true if it is acceptable, false otherwise
   */
  public boolean isAcceptableDetection(String sourceText, String langHint, float confidence) {
    List<ThresholdRangeConfiguration> confidenceThresholds = StringUtils.isBlank(langHint)
        ? getNoHintThresholds()
        : getHintThresholds();
    for (ThresholdRangeConfiguration threshold : confidenceThresholds) {
      if (threshold.isApplicableToText(sourceText)) 
        return threshold.isAcceptableDetection(confidence);
    }
    return false;
  }

  
  
  
}
