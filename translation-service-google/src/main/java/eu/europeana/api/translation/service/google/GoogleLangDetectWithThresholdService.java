package eu.europeana.api.translation.service.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.translate.v3.DetectedLanguage;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Language detection service extending the google language detection service
 * with support for language hint and text-length-based confidence threshold
 * 
 * @author Nuno Freire
 *
 */
public class GoogleLangDetectWithThresholdService extends BaseGoogleLangDetectService {
  List<ThresholdConfiguration> confidenceThresholdsWithHint;
  List<ThresholdConfiguration> confidenceThresholdsWithoutHint;

  public GoogleLangDetectWithThresholdService(String googleProjectId,
      GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(googleProjectId, clientWrapperBean);
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
    if (this.googleProjectId.equals(GoogleTranslationServiceClientWrapper.MOCK_CLIENT_PROJ_ID)) {
      String langHint = languageDetectionObjs.get(0).getHint();
      String value = StringUtils.isNotBlank(langHint) ? langHint : "en";
      for (LanguageDetectionObj obj : languageDetectionObjs) {
        obj.setDetectedLang(value);
      }
    } else
      super.detectLang(languageDetectionObjs);
  }

  /**
   * Accepts/rejects the highest confidence detected language based on the length
   * of text and the confidence
   */
  protected String chooseDetectedLang(String sourceText, List<DetectedLanguage> detectedLanguages, String langHint) {
    if (detectedLanguages.isEmpty()) {
      return null;
    }

    List<ThresholdConfiguration> confidenceThresholds = StringUtils.isBlank(langHint) ? confidenceThresholdsWithoutHint
        : confidenceThresholdsWithHint;

    String detectedLang = detectedLanguages.get(0).getLanguageCode();
    float confidence = detectedLanguages.get(0).getConfidence();
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
    GLangDetectServiceConfiguration config = null;
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

  private GLangDetectServiceConfiguration parseConfig(BufferedReader reader) throws JsonProcessingException {
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    return new ObjectMapper().readValue(content, GLangDetectServiceConfiguration.class);
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
