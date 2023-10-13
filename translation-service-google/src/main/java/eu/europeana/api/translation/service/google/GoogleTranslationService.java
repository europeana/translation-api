package eu.europeana.api.translation.service.google;

import java.util.ArrayList;
import java.util.List;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextRequest.Builder;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import eu.europeana.api.translation.definitions.service.TranslationService;
import eu.europeana.api.translation.definitions.service.exception.TranslationException;

/**
 * Translation service implementing remote invocation of google language detection service
 *  Note that this requires the GOOGLE_APPLICATION_CREDENTIALS environment variable to be available
 * as well as a projectId (defined in the application properties).
 * @author GordeaS
 *
 */
public class GoogleTranslationService implements TranslationService {

  private static final String MIME_TYPE_TEXT = "text/plain";
  private final String googleProjectId;

  private GoogleTranslationServiceClientWrapper clientWrapper;
  private LocationName locationName;
  private String serviceId;

  public GoogleTranslationService(String googleProjectId, GoogleTranslationServiceClientWrapper clientWrapperBean) {
    this.googleProjectId = googleProjectId;
    this.locationName = LocationName.of(googleProjectId, "global");
    this.clientWrapper = clientWrapperBean;
  }
  
  /**
   * used mainly for testing purposes. 
   * @param client
   */
  public void init(GoogleTranslationServiceClientWrapper clientWrapper) {
    this.clientWrapper = clientWrapper;
    this.locationName = LocationName.of(getGoogleProjectId(), "global");
  }

  @Override
  public List<String> translate(List<String> text, String targetLanguage)
      throws TranslationException {
    return translate(text, targetLanguage, null);
  }

  @Override
  public List<String> translate(List<String> text, String targetLanguage, String sourceLanguage) throws TranslationException {
    try {
      List<String> result = new ArrayList<>();
      if(text.isEmpty()) {
        return result;
      }
      
      Builder requestBuilder = TranslateTextRequest.newBuilder().setParent(locationName.toString())
          .setMimeType(MIME_TYPE_TEXT).setTargetLanguageCode(targetLanguage).addAllContents(text);
      if (sourceLanguage != null) {
        requestBuilder.setSourceLanguageCode(sourceLanguage);
      }
      TranslateTextRequest request = requestBuilder.build();
  
      TranslateTextResponse response = this.clientWrapper.getClient().translateText(request);

      for (Translation t : response.getTranslationsList()) {
        result.add(t.getTranslatedText());
      }
      return result;
    } catch (ApiException ex) {
      final int remoteStatusCode = ex.getStatusCode().getCode().getHttpStatusCode();
      throw new TranslationException("Exception occured during Google translation!", remoteStatusCode, ex);
    } 
  }

  @Override
  public boolean isSupported(String srcLang, String targetLanguage) {
    return true;
  }

  @Override
  public String getExternalServiceEndPoint() {
    return "/" + getGoogleProjectId();
  }

  public String getGoogleProjectId() {
    return googleProjectId;
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
  public void close() {
    this.clientWrapper.close();
  }
}
