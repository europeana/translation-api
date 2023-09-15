package eu.europeana.api.translation.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectLanguageRequest.Builder;
import com.google.cloud.translate.v3.DetectLanguageResponse;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslationServiceClient;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

public class GoogleLangDetectService implements LanguageDetectionService {
   
  protected static final Logger LOG = LogManager.getLogger(GoogleLangDetectService.class);
  private TranslationServiceClient client;
  private final String googleProjectId;
  private LocationName locationName;
  private String serviceId;

  /**
   * used mainly for testing purposes. 
   * @param client
   */
  public void init(TranslationServiceClient client) {
    this.client = client;
    this.locationName = LocationName.of(googleProjectId, "global");
  }
  
  public GoogleLangDetectService(String googleProjectId, TranslationServiceClient clientBean) {
    this.googleProjectId=googleProjectId;
    this.locationName = LocationName.of(googleProjectId, "global");
    this.client=clientBean;
  }

  @Override
  public boolean isSupported(String srcLang) {
     return true;
  }

  @Override
  public List<String> detectLang(List<String> texts, String langHint) throws LanguageDetectionException {
    //docs: https://cloud.google.com/translate/docs/advanced/detecting-language-v3#translate_v3_detect_language-java  
    try {
      List<String> result = new ArrayList<>();
      Builder googleLangDetectBuilder = DetectLanguageRequest.newBuilder();
      googleLangDetectBuilder.setParent(locationName.toString());
      googleLangDetectBuilder.setMimeType("text/plain");
      for(String text : texts) {
        DetectLanguageRequest request = googleLangDetectBuilder
            .setContent(text)
            .build();        

        DetectLanguageResponse response = client.detectLanguage(request);

        //Display list of detected languages sorted by detection confidence. The most probable language is first.
        //The language detected: getLanguageCode()
        // Confidence of detection result for this language: getConfidence()
        if(response.getLanguagesList()==null || response.getLanguagesList().isEmpty()) {
          result.add(null);
        }
        else {
          result.add(response.getLanguagesList().get(0).getLanguageCode());
        }
      }
      return result;
    } catch (Exception ex) {
      throw new LanguageDetectionException(ex.getMessage());
    }
  }
  
  @Override
  public void close() {
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
