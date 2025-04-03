package eu.europeana.api.translation.service.tika;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.language.detect.LanguageResult;

import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.threshold.ThresholdRangeConfiguration;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

/**
 * Language detection service extending the Apache Tika language detection
 * service with support for language hint and text-length-based confidence
 * thresholds
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class ApacheTikaLangDetectWithThresholdsService extends BaseApacheTikaLangDetectService {
  
  ThresholdsConfiguration thresholdsConf;

  /**
   * Default constructor
   */
  public ApacheTikaLangDetectWithThresholdsService() {
    super();
  }

  /**
   * Accepts/rejects the highest confidence detected language based on the length
   * of text and the confidence given by Tika
   */
  protected String chooseDetectedLang(String sourceText, List<LanguageResult> tikaLanguages, String langHint) {
    if (tikaLanguages.isEmpty())
      return null;
    String detectedLang = tikaLanguages.get(0).getLanguage();
    float confidence = tikaLanguages.get(0).getRawScore();
    if (thresholdsConf.isAcceptableDetection(sourceText, langHint, confidence))
      return detectedLang;
    else
      return StringUtils.isBlank(langHint) ? null : langHint;
  }

  @Override
  public void close() {
    // nothing to do
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

  @Override
  public void setConfiguration(Map<String, LanguageDetectionService> detectionServices, String configResourceName)
      throws LangDetectionServiceConfigurationException {
    thresholdsConf=ThresholdsConfiguration.fromJson(configResourceName);
  }

}
