package eu.europeana.api.translation.service.tika;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.language.detect.LanguageResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;

/**
 * Language detection service extending the Apache Tika language detection
 * service with support for language hint and text-length-based confidence
 * thresholds
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class ApacheTikaLangDetectWithThresholdsService extends ApacheTikaLangDetectService {
  List<ThresholdConfiguration> confidenceThresholdsWithHint = null;
  List<ThresholdConfiguration> confidenceThresholdsWithoutHint = null;

  public ApacheTikaLangDetectWithThresholdsService(List<ThresholdConfiguration> confidenceThresholdsWithHint,
      List<ThresholdConfiguration> confidenceThresholdsWithoutHint) {
    super();
  }

  /**
   * Accepts/rejects the highest confidence detected language based on the length
   * of text and the confidence given by Tika
   */
  protected String chooseDetectedLang(String sourceText, List<LanguageResult> tikaLanguages, String langHint) {
    if (tikaLanguages.isEmpty()) {
      return null;
    }

    List<ThresholdConfiguration> confidenceThresholds = StringUtils.isBlank(langHint) ? confidenceThresholdsWithoutHint
        : confidenceThresholdsWithHint;

    String detectedLang = tikaLanguages.get(0).getLanguage();
    float confidence = tikaLanguages.get(0).getRawScore();
    for (ThresholdConfiguration threshold : confidenceThresholds) {
      Boolean acceptDetection = threshold.acceptDetection(sourceText, confidence);
      if (acceptDetection != null) {
        if (acceptDetection)
          return detectedLang;
        else
          return StringUtils.isBlank(langHint) ? null : langHint;
      }
    }
    return null;
  }

  @Override
  public void setConfiguration(Map<String, LanguageDetectionService> detectionServices, String configResourceName)
      throws LangDetectionServiceConfigurationException {
    ApacheTikaServiceConfiguration config = null;
    try (InputStream inputStream = getClass().getResourceAsStream(configResourceName)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      config = parseConfig(reader);
      LOG.info("Successfully loaded service configurations from classpath resources.");
    } catch (IOException e) {
      throw new LangDetectionServiceConfigurationException(
          "Cannot read service configurations from classpath resource!", e);
    }
    confidenceThresholdsWithHint = config.getHintThresholds();
    confidenceThresholdsWithoutHint = config.getNoHintThresholds();
    validateThresholds(confidenceThresholdsWithHint);
    validateThresholds(confidenceThresholdsWithoutHint);
  }

  private ApacheTikaServiceConfiguration parseConfig(BufferedReader reader) throws JsonProcessingException {
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    return new ObjectMapper().readValue(content, ApacheTikaServiceConfiguration.class);
  }

  private void validateThresholds(List<ThresholdConfiguration> confidenceThresholds)
      throws LangDetectionServiceConfigurationException {
    ThresholdConfiguration previous = null;
    for (ThresholdConfiguration threshold : confidenceThresholds) {
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
}
