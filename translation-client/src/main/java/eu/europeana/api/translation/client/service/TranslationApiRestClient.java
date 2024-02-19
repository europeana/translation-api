package eu.europeana.api.translation.client.service;

import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.LangDetectResponse;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

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
    private static  final String ERROR_MESSAGE = "Translation API Client call failed - ";
    private final WebClient webClient;

    /**
     * constructor to initialise webclient
     * @param apiClient client for rest request
     */
    public TranslationApiRestClient(WebClient apiClient) {
        this.webClient = apiClient;
    }

    /**
     * Returns the Translation api translate endpoint response
     *
     * @param request http request
     * @param authToken token for authentication
     * @return TranslationResponse
     * @throws TranslationApiException throws an exception if json is invalid or Translation api is not up and running
     */
    public TranslationResponse getTranslations(String request, String authToken) throws TranslationApiException {
        return getTranslationApiResponse(webClient, buildUrl(TRANSLATE_URL), request, false, authToken);
    }

    /**
     * Retruns the translation api lang detection response
     *
     * @param request http request
     * @param authToken token for authentication
     * @return TranslationResponse
     * @throws TranslationApiException throws an exception if json is invalid or Translation api is not up and running
     */
    public LangDetectResponse getDetectedLanguages(String request, String authToken) throws TranslationApiException {
        return getTranslationApiResponse(webClient, buildUrl(LANG_DETECT_URL), request, true, authToken);
    }

    /**
     * Get the supported languages for detection and supported language pairs for translations
     * @param supportedLanguagesForDetection languages for lang detect
     * @param supportedLanguagesForTranslation languages for translations
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
        } catch (WebClientResponseException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(ERROR_MESSAGE + " {} ", e.getMessage());
            }
            throw new TranslationApiException(ERROR_MESSAGE + e.getMessage(), e.getRawStatusCode(), e);
        }
    }

    /**
     * Executes the post request for both endpoint "translate" and "detect"
     *
     * @param webClient webclient to exceute
     * @param uriBuilderURIFunction uri of the translation api to be executed
     * @param jsonBody  request body for post
     * @param langDetect true, if lang detect request
     * @param authToken   - the JWT token used for invocation of translation API
     * @param <T>
     * @throws TranslationApiException throws an exception if json is invalid or Translation api is not up and running
     * @return LangDetectResponse or TranslationResponse
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

        } catch (WebClientResponseException e) {
            String message = getErrorMessage(e.getResponseBodyAsString(), e.getMessage());
            LOGGER.debug(ERROR_MESSAGE + " {} ", message);
            throw new TranslationApiException(ERROR_MESSAGE + message, e.getRawStatusCode(), e);
        }
    }


    private WebClient.ResponseSpec executePost(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction, String jsonBody, String authToken) {
        return webClient
                .post()
                .uri(uriBuilderURIFunction)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve();
    }

    private WebClient.ResponseSpec executeGet(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction, String authToken) {
        return webClient
                .get()
                .uri(uriBuilderURIFunction)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .retrieve();
    }

    private String getErrorMessage(String errorResponse, String defaultMessage) {
        if (StringUtils.isNotEmpty(errorResponse)&& errorResponse.contains("message")) {
            return StringUtils.substringBetween(errorResponse, "\"message\":", "\",");
        }
        return defaultMessage;
    }
}
