package eu.europeana.api.translation.client.service;

import eu.europeana.api.translation.client.exception.ExternalServiceException;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.LangDetectResponse;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.Exceptions;

import java.net.URI;
import java.util.Set;
import java.util.function.Function;

import static eu.europeana.api.translation.client.utils.TranslationClientUtils.*;

/**
 * Translation API rest client for the rest request
 * @author srishti singh
 */
public class TranslationApiRestClient {

    private static final Logger LOGGER = LogManager.getLogger(TranslationApiRestClient.class);
    private final WebClient webClient;

    public TranslationApiRestClient(WebClient apiClient) {
        this.webClient = apiClient;
    }

    /**
     * Returns the Translation api translate endpoint response
     *
     * @param request
     * @return
     */
    public TranslationResponse getTranslations(String request, String authToken) throws TranslationApiException {
        return getTranslationApiResponse(webClient, buildUrl(TRANSLATE_URL), request, false, authToken);
    }

    /**
     * Retruns the translation api lang detection response
     *
     * @param request
     * @return
     */
    public LangDetectResponse getDetectedLanguages(String request, String authToken) throws TranslationApiException {
        return getTranslationApiResponse(webClient, buildUrl(LANG_DETECT_URL), request, true, authToken);
    }

    /**
     * Get the supported languages for detection and supported language pairs for translations
     * @param supportedLanguagesForDetection
     * @param supportedLanguagesForTranslation
     * @throws TranslationApiException throws an exception if json is invalid or Translation api is not up and running
     */
    public void getSupportedLanguages(Set<String> supportedLanguagesForDetection, Set<LanguagePair> supportedLanguagesForTranslation)
            throws TranslationApiException {
        String json = getInfoEndpointResponse();
        getDetectionLanguages(json, supportedLanguagesForDetection);
        getTranslationLanguagePairs(json, supportedLanguagesForTranslation);
    }

    /**
     * Executes the get request for the info endpoint of translation api
     *
     * @return
     */
    private String getInfoEndpointResponse() throws TranslationApiException {
        try {
            WebClient.ResponseSpec result = executeGet(webClient, buildUrl(INFO_ENDPOINT_URL), null);
            return result
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Translation API Client call failed - {}", e.getMessage());
            }
            throw new TranslationApiException("Translation API Client call failed - " + e.getMessage(), e);
        }
    }

    /**
     * Executes the post request for both endpoint "translate" and "detect"
     *
     * @param webClient
     * @param uriBuilderURIFunction
     * @param jsonBody
     * @param langDetect
     * @param authToken             - the JWT token used for invocation of translation API
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getTranslationApiResponse(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction, String jsonBody,
                                           boolean langDetect, String authToken) throws TranslationApiException {
        try {
            WebClient.ResponseSpec result = executePost(webClient, uriBuilderURIFunction, jsonBody, authToken);
            if (langDetect) {
                return (T) result
                        .bodyToMono(LangDetectResponse.class)
                        .block();
            } else {
                return (T) result
                        .bodyToMono(TranslationResponse.class)
                        .block();
            }

        } catch (Exception e) {
            /*
             * Spring WebFlux wraps exceptions in ReactiveError (see Exceptions.propagate())
             * So we need to unwrap the underlying exception, for it to be handled by callers of this method
             **/
            Throwable t = Exceptions.unwrap(e);

            if (t instanceof ExternalServiceException) {
                throw new ExternalServiceException(e.getMessage(), e);
            }
            LOGGER.debug("Translation API Client call failed - {}", e.getMessage());
            throw new TranslationApiException("Translation API Client call failed - " + e.getMessage(), e);
        }
    }


    private WebClient.ResponseSpec executePost(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction, String jsonBody, String authToken) {
        return webClient
                .post()
                .uri(uriBuilderURIFunction)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .onStatus(
                        HttpStatus.GATEWAY_TIMEOUT::equals,
                        response -> response.bodyToMono(String.class).map(ExternalServiceException::new));
    }

    private WebClient.ResponseSpec executeGet(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction, String authToken) {
        return webClient
                .get()
                .uri(uriBuilderURIFunction)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .retrieve();
    }
}
