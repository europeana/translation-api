package eu.europeana.api.translation.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.tasks.v2.stub.CloudTasksStubSettings;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextRequest.Builder;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import eu.europeana.api.translation.service.exception.TranslationException;

/**
 * Note that this requires the GOOGLE_APPLICATION_CREDENTIALS environment variable to be available
 * as well as a projectId (defined in the application properties).
 */
public class GoogleTranslationService implements TranslationService {

  private static final Logger LOG = LogManager.getLogger(GoogleTranslationService.class);
  private static final String MIME_TYPE_TEXT = "text/plain";
  private final String googleProjectId;

  private TranslationServiceClient client;
  private LocationName locationName;

  public GoogleTranslationService(String googleProjectId) {
    this(googleProjectId, true, false);
  }

  public GoogleTranslationService(String googleProjectId, boolean initClientConnection,
      boolean useHttpClient) {
    this.googleProjectId = googleProjectId;
    if (initClientConnection) {
      init(useHttpClient);
    }
  }

  /**
   * Creates a new client that can send translation requests to Google Cloud Translate. Note that
   * the client needs to be closed when it's not used anymore
   * 
   * @throws RuntimeException when there is a problem creating the client
   */
  public void init(boolean useHttpClient) throws RuntimeException {
    try {

      // gRPC doesn't like communication via the socks proxy (throws an error) and also doesn't
      // support the
      // socksNonProxyHosts settings, so this is to tell it to by-pass the configured proxy
      if (useHttpClient) {
        TranslationServiceSettings translationServiceSettings =
            TranslationServiceSettings.newHttpJsonBuilder().build();
        this.client = TranslationServiceClient.create(translationServiceSettings);
      } else {
        TransportChannelProvider transportChannelProvider = CloudTasksStubSettings
            .defaultGrpcTransportProviderBuilder()
            .setChannelConfigurator(
                managedChannelBuilder -> managedChannelBuilder.proxyDetector(socketAddress -> null))
            .build();

        TranslationServiceSettings tss;
        tss = TranslationServiceSettings.newBuilder()
            .setTransportChannelProvider(transportChannelProvider).build();

        this.client = TranslationServiceClient.create(tss);
      }

      this.locationName = LocationName.of(getGoogleProjectId(), "global");
      LOG.info("GoogleTranslationService initialised, projectId = {}", getGoogleProjectId());
    } catch (IOException e) {
      throw new RuntimeException("Cannot instantiate Google TranslationServiceClient!", e);
    }
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
  public void close() {
    if (this.client != null) {
      if(LOG.isDebugEnabled()) {
        LOG.debug("Shutting down GoogleTranslationService client...");
      }
      try {
        this.client.close();
      } catch (RuntimeException e) {
        if(LOG.isInfoEnabled()) {
        LOG.info("Unexpected error occured when closing translation service: {}", getServiceId(), e);
        }
      }
    }
  }

  @Override
  public List<String> translate(List<String> texts, String targetLanguage)
      throws TranslationException {
    return translate(texts, targetLanguage, null);
  }

  @Override
  public List<String> translate(List<String> text, String targetLanguage, String sourceLanguage) {
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
    return "GOOGLE";
  }
}
