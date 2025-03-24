package eu.europeana.api.translation.service.google;

import java.util.List;

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
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Translation service implementing remote invocation of google language
 * detection service
 * 
 * @author GordeaS
 *
 */
public abstract class BaseGoogleLangDetectService implements LanguageDetectionService {

  protected static final Logger LOG = LogManager.getLogger(BaseGoogleLangDetectService.class);
  private GoogleTranslationServiceClientWrapper clientWrapper;
  protected final String googleProjectId;
  private LocationName locationName;
  private String serviceId;

  /**
   * used mainly for testing purposes.
   * 
   * @param client
   */
  public void init(GoogleTranslationServiceClientWrapper clientWrapper) {
    this.clientWrapper = clientWrapper;
    this.locationName = LocationName.of(googleProjectId, "global");
  }

  /**
   * Main constructor
   * 
   * @param googleProjectId project ID
   * @param clientWrapperBean Client wrapper
   */
  protected BaseGoogleLangDetectService(String googleProjectId, GoogleTranslationServiceClientWrapper clientWrapperBean) {
    this.googleProjectId = googleProjectId;
    this.locationName = LocationName.of(googleProjectId, "global");
    this.clientWrapper = clientWrapperBean;
  }

  @Override
  public boolean isSupported(String srcLang) {
    return true;
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
    // docs:
    // https://cloud.google.com/translate/docs/advanced/detecting-language-v3#translate_v3_detect_language-java
    try {
      if (languageDetectionObjs.isEmpty()) {
        return;
      }

      Builder googleLangDetectBuilder = DetectLanguageRequest.newBuilder();
      googleLangDetectBuilder.setParent(locationName.toString());
      googleLangDetectBuilder.setMimeType("text/plain");
      for (LanguageDetectionObj object : languageDetectionObjs) {
        DetectLanguageRequest request = googleLangDetectBuilder.setContent(object.getText()).build();

        DetectLanguageResponse response = clientWrapper.getClient().detectLanguage(request);

        object.setDetectedLang(chooseDetectedLang(object.getText(), response.getLanguagesList(), object.getHint()));
      }
    } catch (ApiException ex) {
      final int remoteStatusCode = ex.getStatusCode().getCode().getHttpStatusCode();
      throw new LanguageDetectionException("Exception occured during Google language detection!", remoteStatusCode, ex);
    }
  }

  /**
   * Return the first one. Subclasses may override this method for choosing with
   * more elaborate methods
   */
  protected abstract String chooseDetectedLang(String sourceText, List<DetectedLanguage> detectedLanguages, String langHint);

  @Override
  public void close() {
    clientWrapper.close();
  }

  @Override
  public String getServiceId() {
    return serviceId;
  }

  @Override
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

}
