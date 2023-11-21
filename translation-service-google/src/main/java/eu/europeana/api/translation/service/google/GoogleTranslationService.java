package eu.europeana.api.translation.service.google;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextRequest.Builder;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

/**
 * Translation service implementing remote invocation of google language detection service
 *  Note that this requires the GOOGLE_APPLICATION_CREDENTIALS environment variable to be available
 * as well as a projectId (defined in the application properties).
 * @author GordeaS
 *
 */
public class GoogleTranslationService extends AbstractTranslationService {

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
  public void translate(List<TranslationObj> translationObjs, boolean detectLanguages) throws TranslationException {
    try {
      if(translationObjs.isEmpty()) {
        return;
      }
      //analyze only objects that still do not have the translation
      List<Integer> validIndexes = IntStream.range(0, translationObjs.size())
          .filter(i -> translationObjs.get(i).getTranslation()==null)
          .boxed()
          .collect(Collectors.toList());
      if(validIndexes.isEmpty()) {
        return;
      }
      List<String> texts = validIndexes.stream()
          .map(el -> translationObjs.get(el).getText())
          .collect(Collectors.toList());
      
      List<String> sourceLangs = validIndexes.stream()
          .filter(el -> translationObjs.get(el).getSourceLang()!=null)
          .map(el -> translationObjs.get(el).getSourceLang())
          .collect(Collectors.toList());
      boolean sameSourceLang = sourceLangs.size()==validIndexes.size() && sourceLangs.stream().distinct().count()==1;
      String targetLang = translationObjs.get(validIndexes.get(0)).getTargetLang();      
      Builder requestBuilder = TranslateTextRequest.newBuilder().setParent(locationName.toString())
          .setMimeType(MIME_TYPE_TEXT).setTargetLanguageCode(targetLang).addAllContents(texts);
      //only set the source language if it is the same for all texts
      if (sameSourceLang) {
        requestBuilder.setSourceLanguageCode(sourceLangs.get(0));
      }
      TranslateTextRequest request = requestBuilder.build();
  
      TranslateTextResponse response = this.clientWrapper.getClient().translateText(request);

      int counter=0;
      for (Translation t : response.getTranslationsList()) {
        if(! sameSourceLang) {
          translationObjs.get(validIndexes.get(counter)).setSourceLang(t.getDetectedLanguageCode());
        }
        translationObjs.get(validIndexes.get(counter)).setTranslation(t.getTranslatedText());
        counter++;
      }
    }
    catch (ApiException ex) {
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

  @Override
  public void detectLanguages(List<TranslationObj> translationObjs)
      throws TranslationException {
  }
}
