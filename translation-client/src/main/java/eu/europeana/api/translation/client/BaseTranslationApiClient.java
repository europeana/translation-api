package eu.europeana.api.translation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.europeana.api.translation.client.config.TranslationClientConfiguration;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.client.service.TranslationApiRestClient;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;
import java.util.TreeSet;

public class BaseTranslationApiClient {

    private static final Logger LOG = LogManager.getLogger(BaseTranslationApiClient.class);
    private static final int MAXIN_MEM_SIZE_MB = 10;

    private final TranslationClientConfiguration configuration;
    private final ObjectWriter objectWriter;
    private static Set<String> supportedLanguagesForDetection = new TreeSet<>();
    private static Set<LanguagePair> supportedLanguagesForTranslation = new TreeSet<>();


    private TranslationApiRestClient translationApiRestClient;

    public BaseTranslationApiClient(TranslationClientConfiguration configuration) throws TranslationApiException {
        this.configuration = configuration;
        if (this.configuration.getTranslationApiUrl() == null || this.configuration.getTranslationApiUrl().isEmpty()) {
            throw new TranslationApiException("Translation api endpoint not configured !!!");
        }
        this.translationApiRestClient = new TranslationApiRestClient(getApiClient(this.configuration.getTranslationApiUrl()));
        this.objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

        // load the supported languages
        this.translationApiRestClient.getSupportedLanguages(supportedLanguagesForDetection, supportedLanguagesForTranslation);
    }

    protected BaseTranslationApiClient() throws TranslationApiException {
        this(new TranslationClientConfiguration());
    }

    private WebClient getApiClient(String apiEndpoint) {
        return WebClient.builder()
                .baseUrl(apiEndpoint)
                .filter(logRequest())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(MAXIN_MEM_SIZE_MB * 1024 * 1024))
                        .build())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            LOG.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return next.exchange(clientRequest);
        };
    }

    public ObjectWriter getObjectWriter() {
        return objectWriter;
    }
    public TranslationClientConfiguration getConfiguration() {
        return configuration;
    }

    public TranslationApiRestClient getTranslationApiRestClient() {
        return translationApiRestClient;
    }

    public Set<String> getSupportedLanguagesForDetection() {
        return supportedLanguagesForDetection;
    }

    public Set<LanguagePair> getSupportedLanguagesForTranslation() {
        return supportedLanguagesForTranslation;
    }
}
