package eu.europeana.api.translation.service;

import java.util.ArrayList;
import java.util.List;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextRequest.Builder;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import eu.europeana.api.translation.service.exception.TranslationException;

/**
 * Note that this requires the GOOGLE_APPLICATION_CREDENTIALS environment variable to be available
 * as well as a projectId (defined in the application properties).
 */
public class GoogleTranslationService implements TranslationService {

  private static final String MIME_TYPE_TEXT = "text/plain";
  private final String googleProjectId;

  private TranslationServiceClient client;
  private LocationName locationName;
  private String serviceId;

  public GoogleTranslationService(String googleProjectId, TranslationServiceClient clientBean) {
    this.googleProjectId = googleProjectId;
    this.locationName = LocationName.of(googleProjectId, "global");
    this.client = clientBean;
  }
  
  /**
   * used mainly for testing purposes. 
   * @param client
   */
  public void init(TranslationServiceClient client) {
    this.client = client;
    this.locationName = LocationName.of(getGoogleProjectId(), "global");
  }

  @Override
  public List<String> translate(List<String> texts, String targetLanguage)
      throws TranslationException {
    return translate(texts, targetLanguage, null);
  }

  @Override
  public List<String> translate(List<String> text, String targetLanguage, String sourceLanguage) throws TranslationException {
    try {
      Builder requestBuilder = TranslateTextRequest.newBuilder().setParent(locationName.toString())
          .setMimeType(MIME_TYPE_TEXT).setTargetLanguageCode(targetLanguage).addAllContents(text);
  
      if (sourceLanguage != null) {
        requestBuilder.setSourceLanguageCode(sourceLanguage);
      }
  
      TranslateTextRequest request = requestBuilder.build();
  
      TranslateTextResponse response = this.client.translateText(request);
      List<String> result = new ArrayList<>();
      for (Translation t : response.getTranslationsList()) {
        result.add(t.getTranslatedText());
      }
      return result;
    }
    catch (Exception ex) {
      throw new TranslationException(ex.getMessage(), ex);
    }
  }

  @Override
  public boolean isSupported(String srcLang, String targetLanguage) {
    if (srcLang == null) {
      // automatic language detection
      return isTargetSupported(targetLanguage);
    }
    return true;
  }

  private boolean isTargetSupported(String targetLanguage) {
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
  }
}
