package eu.europeana.api.translation.service.tika;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Definition of a confidence threshold applicable to a text length range
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdConfiguration {
  /**
   * The minimum text length where the threshold is applicable (inclusive). Must
   * never be null.
   */
  private Integer minLength;

  /**
   * The maximum text length where the threshold is applicable (inclusive). null
   * means unbounded
   */
  private Integer maxLength;

  /**
   * The minimum confidence required for accepting a detection (inclusive). null
   * means that results in this range should always be rejected.
   */
  private Double confidenceThreshold;

  public ThresholdConfiguration() {
    super();
  }

  /**
   * Checks if a detected language is above the threshold
   * 
   * @param sourceText the text where a language was detected
   * @param confidence the confidence on the detected language
   * @return true, when the detection is accepted, false otherwise. If this
   *         threshold is not applicable to the source text, returns null.
   */
  public Boolean acceptDetection(String sourceText, double confidence) {
    if ((sourceText.length() >= minLength) && (maxLength == null || (sourceText.length() <= maxLength)))
      return (confidenceThreshold != null && (confidence >= confidenceThreshold));
    return null;
  }

  @JsonGetter(ApacheTikaConstants.MIN_LENGTH)
  public Integer getMinLength() {
    return minLength;
  }

  @JsonSetter(ApacheTikaConstants.MIN_LENGTH)
  public void setMinLength(Integer minLength) {
    this.minLength = minLength;
  }

  @JsonGetter(ApacheTikaConstants.MAX_LENGTH)
  public Integer getMaxLength() {
    return maxLength;
  }

  @JsonSetter(ApacheTikaConstants.MAX_LENGTH)
  public void setMaxLength(Integer maxLength) {
    this.maxLength = maxLength;
  }

  @JsonGetter(ApacheTikaConstants.CONFIDENCE_THRESHOLD)
  public Double getConfidenceThreshold() {
    return confidenceThreshold;
  }

  @JsonSetter(ApacheTikaConstants.CONFIDENCE_THRESHOLD)
  public void setConfidenceThreshold(Double confidenceThreshold) {
    this.confidenceThreshold = confidenceThreshold;
  }
}
