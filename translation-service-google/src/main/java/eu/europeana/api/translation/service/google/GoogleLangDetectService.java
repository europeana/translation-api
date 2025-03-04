package eu.europeana.api.translation.service.google;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectLanguageRequest.Builder;
import com.google.cloud.translate.v3.DetectLanguageResponse;
import com.google.cloud.translate.v3.DetectedLanguage;
import com.google.cloud.translate.v3.LocationName;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Translation service implementing remote invocation of google language
 * detection service
 * 
 * @author GordeaS
 *
 */
public class GoogleLangDetectService extends BaseGoogleLangDetectService {

  public GoogleLangDetectService(String googleProjectId, GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(googleProjectId, clientWrapperBean);
  }

  @Override
  public void setConfiguration(Map<String, LanguageDetectionService> detectionServices, String configResourceName)
      throws LangDetectionServiceConfigurationException {
    // nothing to do
  }

  /**
   * Return the first one. Subclasses may override this method for choosing with
   * more elaborate methods
   */
  protected String chooseDetectedLang(String sourceText, List<DetectedLanguage> detectedLanguages, String langHint) {
    // Display list of detected languages sorted by detection confidence. The most
    // probable language is first.
    // The language detected: getLanguageCode()
    // Confidence of detection result for this language: getConfidence()
    if (detectedLanguages == null || detectedLanguages.isEmpty()) {
      return null;
    } else {
      return detectedLanguages.get(0).getLanguageCode();
    }
  }


}
