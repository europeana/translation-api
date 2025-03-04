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
import eu.europeana.api.translation.service.threshold.ThresholdRangeConfiguration;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

/**
 * Language detection service extending the google language detection service
 * with support for language hint and text-length-based confidence threshold
 * 
 * @author Nuno Freire
 *
 */
public class GoogleLangDetectWithThresholdService extends BaseGoogleLangDetectService {
  ThresholdsConfiguration thresholdsConf;

  public GoogleLangDetectWithThresholdService(String googleProjectId,
      GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(googleProjectId, clientWrapperBean);
  }

  /**
   * Accepts/rejects the highest confidence detected language based on the length
   * of text and the confidence
   */
  protected String chooseDetectedLang(String sourceText, List<DetectedLanguage> detectedLanguages, String langHint) {
    if (detectedLanguages.isEmpty()) {
      return null;
    }

    List<ThresholdRangeConfiguration> confidenceThresholds = StringUtils.isBlank(langHint) ? thresholdsConf.getNoHintThresholds()
        : thresholdsConf.getHintThresholds();

    String detectedLang = detectedLanguages.get(0).getLanguageCode();
    float confidence = detectedLanguages.get(0).getConfidence();
    for (ThresholdRangeConfiguration threshold : confidenceThresholds) {
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
      thresholdsConf=ThresholdsConfiguration.fromJson(configResourceName);
  }
}
