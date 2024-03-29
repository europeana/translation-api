package eu.europeana.api.translation.service.google;

import java.util.List;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectLanguageRequest.Builder;
import com.google.cloud.translate.v3.DetectLanguageResponse;
import com.google.cloud.translate.v3.LocationName;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Translation service implementing remote invocation of google language detection service
 * @author GordeaS
 *
 */
public class GoogleLangDetectService implements LanguageDetectionService {
   
  protected static final Logger LOG = LogManager.getLogger(GoogleLangDetectService.class);
  private GoogleTranslationServiceClientWrapper clientWrapper;
  private final String googleProjectId;
  private LocationName locationName;
  private String serviceId;

  /**
   * used mainly for testing purposes. 
   * @param client
   */
  public void init(GoogleTranslationServiceClientWrapper clientWrapper) {
    this.clientWrapper = clientWrapper;
    this.locationName = LocationName.of(googleProjectId, "global");
  }
  
  public GoogleLangDetectService(String googleProjectId, GoogleTranslationServiceClientWrapper clientWrapperBean) {
    this.googleProjectId=googleProjectId;
    this.locationName = LocationName.of(googleProjectId, "global");
    this.clientWrapper=clientWrapperBean;
  }

  @Override
  public boolean isSupported(String srcLang) {
     return true;
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
    //docs: https://cloud.google.com/translate/docs/advanced/detecting-language-v3#translate_v3_detect_language-java
    try {
      if (languageDetectionObjs.isEmpty()) {
        return;
      }
      
      Builder googleLangDetectBuilder = DetectLanguageRequest.newBuilder();
      googleLangDetectBuilder.setParent(locationName.toString());
      googleLangDetectBuilder.setMimeType("text/plain");
      for(LanguageDetectionObj object : languageDetectionObjs) {
        DetectLanguageRequest request = googleLangDetectBuilder
            .setContent(object.getText())
            .build();        

        DetectLanguageResponse response = clientWrapper.getClient().detectLanguage(request);

        //Display list of detected languages sorted by detection confidence. The most probable language is first.
        //The language detected: getLanguageCode()
        // Confidence of detection result for this language: getConfidence()
        if(response.getLanguagesList()==null || response.getLanguagesList().isEmpty()) {
          object.setDetectedLang(null);
        }
        else {
          object.setDetectedLang(response.getLanguagesList().get(0).getLanguageCode());
        }
      }
    } catch (ApiException ex) {
      final int remoteStatusCode = ex.getStatusCode().getCode().getHttpStatusCode();
      throw new LanguageDetectionException("Exception occured during Google language detection!", remoteStatusCode, ex);
    }
  }
  
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
    this.serviceId=serviceId;
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }    

}
