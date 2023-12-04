package eu.europeana.api.translation.client.service;

import eu.europeana.api.translation.client.exception.InvalidParamValueException;
import eu.europeana.api.translation.client.exception.TechnicalRuntimeException;
import eu.europeana.api.translation.client.utils.TranslationClientUtils;
import eu.europeana.api.translation.definitions.model.LangDetectResponse;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
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
            if (t instanceof InvalidParamValueException) {
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
                .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1SW55MldXRkhZQ1YxcFNNc0NKXzl2LVhaUUgwUk84c05KNUxLd2JHcmk0In0.eyJleHAiOjE3MDE0NDU3NTgsImlhdCI6MTcwMTQ0MjE1OCwianRpIjoiYjA4MDA2ZTUtNGFmNy00ZmI3LTg2NTYtZWU5YzY3NThjMWQzIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLmV1cm9wZWFuYS5ldS9hdXRoL3JlYWxtcy9ldXJvcGVhbmEiLCJhdWQiOlsiZW50aXRpZXMiLCJ1c2Vyc2V0cyIsImFjY291bnQiXSwic3ViIjoiMThjNTY1ZDMtNmYzZS00NmViLTlkMmYtOTU0ODI5ZGVlYjkyIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdF9jbGllbnRfdHJhbnNsYXRpb25zIiwic2Vzc2lvbl9zdGF0ZSI6IjFlMmVlZGMxLTVlZTMtNDRhYi04MjYzLTgwODJiYjI1MDc0MyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImVudGl0aWVzIjp7InJvbGVzIjpbImVkaXRvciJdfSwidXNlcnNldHMiOnsicm9sZXMiOlsidXNlciJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwiZGVsZXRlLWFjY291bnQiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImNsaWVudF9pbmZvIHByb2ZpbGUgdHJhbnNsYXRpb25zIGVtYWlsIiwic2lkIjoiMWUyZWVkYzEtNWVlMy00NGFiLTgyNjMtODA4MmJiMjUwNzQzIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJFbnRpdHkgRWRpdG9yIFJ1bnNjb3BlIFRlc3QiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0X2VudGl0eV9lZGl0b3IiLCJnaXZlbl9uYW1lIjoiRW50aXR5IEVkaXRvciIsImZhbWlseV9uYW1lIjoiUnVuc2NvcGUgVGVzdCIsImVtYWlsIjoidGVzdF9lbnRpdHlfZWRpdG9yQGV1cm9wZWFuYS5ldSJ9.Wk60gb_4rl7fiHhgRBIKxrvMZpvbC0H9g_Iq6w6sHXudl8I_rsLhQMRkfvTpXxubkJcTt6rFVQAOtfpnVc_4LhVIuCD7KwkihTSvhzbdvP29K7jH3-VoG63wZPnk666OsHd4h98P7UlIetBKixVHb13CuctIVZ5LdjPuNfe4eFWO491GjiE58cLh8JNe7Z2AdR2NWDWLugIU5yrvMkwl-Ono4PjnWycX8r7SfB7XaXbnvjTBtn8SiiM5EUi9MWxrkyynCFvlXsikKypsQgNVYNgITTGPvRQrdOY6ls6mBAajpjWSd0nq2XOYX19SXIM2zJ5va58OIWAI4K9XaFS8tw")
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .onStatus(
                        HttpStatus.UNAUTHORIZED::equals,
                        response -> response.bodyToMono(String.class).map(TechnicalRuntimeException::new))
                .onStatus(HttpStatus.BAD_REQUEST:: equals,
                        response -> response.bodyToMono(String.class).map(InvalidParamValueException::new));

    }
}
