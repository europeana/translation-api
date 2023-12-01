package eu.europeana.api.translation.client.service;

import eu.europeana.api.translation.client.exception.TechnicalRuntimeException;
import eu.europeana.api.translation.client.utils.TranslationClientUtils;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.web.exception.ParamValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.Exceptions;

import java.net.URI;
import java.util.function.Function;
import static eu.europeana.api.translation.client.utils.TranslationClientUtils.*;

public class TranslationApiRestClient {

    private static final Logger LOGGER = LogManager.getLogger(TranslationApiRestClient.class);
    private final WebClient webClient;

    public TranslationApiRestClient(WebClient apiClient) {
        this.webClient = apiClient;
    }

    /**
     * Returns the Translation api translate endpoint response
     * @param request
     * @return
     */
    public TranslationResponse getTranslations(String request) {
       return getTranslationApiResponse(webClient, TranslationClientUtils.buildUrl(TRANSLATE_URL), request, false);
    }

    /**
     * Retruns the translation api lang detection response
     * @param request
     * @return
     */
    public LangDetectResponse getDetectedLanguages(String request) {
        return getTranslationApiResponse(webClient, TranslationClientUtils.buildUrl(LANG_DETECT_URL), request, true);
    }


    /**
     * Executes the post request for both endpoint "translate" and "detect"
     * @param webClient
     * @param uriBuilderURIFunction
     * @param jsonBody
     * @param langDetect
     * @param <T>
     * @return
     */
    public <T> T getTranslationApiResponse(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction, String jsonBody, boolean langDetect) {
        try {
            WebClient.ResponseSpec result = executePost(webClient, uriBuilderURIFunction, jsonBody);
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
            if (t instanceof TechnicalRuntimeException) {
                throw new TechnicalRuntimeException("User is not authorised to perform this action");
            }
            if (t instanceof ParamValidationException) {
                LOGGER.debug("Invalid request body - {} ", e.getMessage());
                return null ;
            }
            // all other exception should be logged and null response should be returned
            LOGGER.debug("Translation API Client call failed - {}", e.getMessage());
            return null;
        }
    }


    private WebClient.ResponseSpec executePost(WebClient webClient, Function<UriBuilder, URI> uriBuilderURIFunction, String jsonBody) throws TechnicalRuntimeException {
        return webClient
                .post()
                .uri(uriBuilderURIFunction)
                .contentType(MediaType.APPLICATION_JSON)
                // TODO need to figure out how we will pass token across API's
                .header("Authorization", " ")
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .onStatus(
                        HttpStatus.UNAUTHORIZED::equals,
                        response -> response.bodyToMono(String.class).map(TechnicalRuntimeException::new));

    }
}
