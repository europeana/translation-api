package eu.europeana.api.translation.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.tasks.v2.stub.CloudTasksStubSettings;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextRequest.Builder;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.language.Language;

/**
 * Note that this requires the GOOGLE_APPLICATION_CREDENTIALS environment variable to be available as well as a projectId
 * (defined in the application properties).
 */
@Service
public class GoogleTranslationService implements TranslationService {

    private static final Logger LOG = LogManager.getLogger(GoogleTranslationService.class);
    private static final String MIME_TYPE_TEXT = "text/plain";

    @Autowired TranslationConfig translationConfig;

    private TranslationServiceClient client;
    private LocationName locationName;
    private List<String> supportedLanguagePairs;

    /**
     * Creates a new client that can send translation requests to Google Cloud Translate. Note that the client needs
     * to be closed when it's not used anymore
     * @throws IOException when there is a problem creating the client
     */
    @PostConstruct
    private void init() throws IOException {
        // gRPC doesn't like communication via the socks proxy (throws an error) and also doesn't support the
        // socksNonProxyHosts settings, so this is to tell it to by-pass the configured proxy
        TransportChannelProvider transportChannelProvider = CloudTasksStubSettings
                .defaultGrpcTransportProviderBuilder()
                .setChannelConfigurator(managedChannelBuilder -> managedChannelBuilder.proxyDetector(socketAddress -> null))
                .build();
        TranslationServiceSettings tss = TranslationServiceSettings.newBuilder()
                .setTransportChannelProvider(transportChannelProvider).build();
        this.client = TranslationServiceClient.create(tss);
        this.locationName = LocationName.of(translationConfig.getTranslationGoogleProjectId(), "global");
        LOG.info("GoogleTranslationService initialised, projectId = {}", translationConfig.getTranslationGoogleProjectId());
    }

    @PreDestroy
    @Override
    public void close() {
        if (this.client != null) {
            LOG.info("Shutting down GoogleTranslationService client...");
            this.client.close();
        }
    }

    public List<String> translate(List<String> texts, String targetLanguage, Language sourceLangHint) {
        TranslateTextRequest request = TranslateTextRequest.newBuilder()
                .setParent(locationName.toString())
                .setMimeType(MIME_TYPE_TEXT)
                .setTargetLanguageCode(targetLanguage)
                .addAllContents(texts)
                .build();
        TranslateTextResponse response = this.client.translateText(request);
        List<String> result = new ArrayList<>();
        for (Translation t : response.getTranslationsList()) {
            result.add(t.getTranslatedText());
        }
        return result;
    }

    @Override
    public List<String> translate(List<String> text, String targetLanguage, String sourceLanguage, boolean detect) {
        Builder requestBuilder = TranslateTextRequest.newBuilder()
            .setParent(locationName.toString())
            .setMimeType(MIME_TYPE_TEXT)
            .setTargetLanguageCode(targetLanguage)
            .addAllContents(text);

        if(sourceLanguage!=null) {
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
    public boolean isSupported(String srcLang, String trgLang) {
      return supportedLanguagePairs.contains(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang);
    }

    @Override
    public void setSupportedLangs(List<String> supportedLangPairs) {
      supportedLanguagePairs=new ArrayList<String>(supportedLangPairs);
    }

}
