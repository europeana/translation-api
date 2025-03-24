package eu.europeana.api.translation.service.threshold;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;

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

  private List<ThresholdRangeConfiguration> hintThresholds;
  private List<ThresholdRangeConfiguration> noHintThresholds;

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


  private void validateThresholds() throws LangDetectionServiceConfigurationException {
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
   * Reads the configuration from a JSON file in the classpath
   * 
   * @param configResourceName JSON file in the classpath
   * @return The configuration object
   * @throws LangDetectionServiceConfigurationException when the configuration is not valid
   */
  public static ThresholdsConfiguration fromJson(String configResourceName)
      throws LangDetectionServiceConfigurationException {
    try (InputStream inputStream = ThresholdsConfiguration.class.getResourceAsStream(configResourceName);
        InputStreamReader rawReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(rawReader)) {
      String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
      ThresholdsConfiguration thresholdsConf = new ObjectMapper().readValue(content, ThresholdsConfiguration.class);
      thresholdsConf.validateThresholds();
      LOG.info("Successfully loaded service configurations from classpath resources.");
      return thresholdsConf;
    } catch (IOException e) {
      throw new LangDetectionServiceConfigurationException(
          "Cannot read service configurations from classpath resource!", e);
    }
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
      Boolean acceptDetection = threshold.acceptDetection(sourceText, confidence);
      if (acceptDetection != null) 
        return acceptDetection;
    }
    return false;
  }
  
  
  
}
