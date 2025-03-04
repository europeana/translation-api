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

  public ApacheTikaLangDetectWithThresholdsService() {
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

    List<ThresholdRangeConfiguration> confidenceThresholds = StringUtils.isBlank(langHint) ? thresholdsConf.getNoHintThresholds()
        : thresholdsConf.getHintThresholds();

    String detectedLang = tikaLanguages.get(0).getLanguage();
    float confidence = tikaLanguages.get(0).getRawScore();
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
