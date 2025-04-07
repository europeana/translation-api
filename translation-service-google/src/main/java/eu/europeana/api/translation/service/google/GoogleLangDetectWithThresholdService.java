package eu.europeana.api.translation.service.google;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.cloud.translate.v3.DetectedLanguage;

import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
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

  @Override
  public void setConfiguration(Map<String, LanguageDetectionService> detectionServices, String configResourceName)
      throws LangDetectionServiceConfigurationException {
    thresholdsConf = ThresholdsConfiguration.fromJson(configResourceName);
  }

  /**
   * Accepts/rejects the highest confidence detected language based on the length
   * of text and the confidence
   */
  protected String chooseDetectedLang(String sourceText, List<DetectedLanguage> detectedLanguages, String langHint) {
    if (detectedLanguages.isEmpty()) 
      return null;
    String detectedLang = detectedLanguages.get(0).getLanguageCode();
    float confidence = detectedLanguages.get(0).getConfidence();
    if (thresholdsConf.isAcceptableDetection(sourceText, langHint, confidence))
      return detectedLang;
    else
      return StringUtils.isBlank(langHint) ? null : langHint;
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }
}
