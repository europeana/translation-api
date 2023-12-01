package eu.europeana.api.translation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.europeana.api.translation.client.config.TranslationClientConfiguration;
import eu.europeana.api.translation.client.service.TranslationApiRestClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

public class BaseTranslationApiClient {

    private static final Logger LOG = LogManager.getLogger(BaseTranslationApiClient.class);
    private static final int maxInMemSizeMb = 10;

    private final TranslationClientConfiguration configuration;
    private final ObjectWriter objectWriter;

    private TranslationApiRestClient translationApiRestClient;

    public BaseTranslationApiClient(TranslationClientConfiguration configuration) {
        this.configuration = configuration;
        this.translationApiRestClient = new TranslationApiRestClient(getApiClient(this.configuration.getTranslationApiUrl()));
        this.objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    }

    protected BaseTranslationApiClient() {
        this(new TranslationClientConfiguration());
    }

    private WebClient getApiClient(String apiEndpoint) {
        return WebClient.builder()
                .baseUrl(apiEndpoint)
                .filter(logRequest())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(maxInMemSizeMb * 1024 * 1024))
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
}
