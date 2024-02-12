package eu.europeana.api.translation.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europeana.api.translation.client.config.TranslationClientConfiguration;
import eu.europeana.api.translation.client.exception.ExternalServiceException;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.client.utils.TranslationClientUtils;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.*;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.exception.TranslationException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Translation API client class
 * Implements the interfaces of Language Detection and translation services
 *
 * @author srishti singh
 */
public class TranslationApiClient extends BaseTranslationApiClient {

    private static final String SERVICE_ID = "TRANSLATION_CLIENT";
    private static final String TOKEN_ERROR_MESSAGE = "Translation API client has not been initialized with a token or has been closed!";

    public static final ThreadLocal<String> token = new ThreadLocal<>();
    // clients
    private final TranslationClient translationClient;
    private final LanguageDetectionClient languageDetectionClient;


    public TranslationApiClient(TranslationClientConfiguration configuration) throws TranslationApiException {
        super(configuration);
        this.translationClient = new TranslationClient();
        this.languageDetectionClient = new LanguageDetectionClient();
    }

    public TranslationService getTranslationService() {
        return this.translationClient;
    }

    public LanguageDetectionService getLanguageDetectionService() {
        return this.languageDetectionClient;
    }

    /**
     * Authentication token for the Translation api requests.
     * @param authToken
     */
    public void setAuthToken(String authToken) {
        token.set(authToken);
    }

    /**
     * Close / purge the token from memory
     */
    public void close() {
        token.remove();
    }


    // Language detection client
    private class LanguageDetectionClient implements LanguageDetectionService {

        @Override
        public boolean isSupported(String srcLang) {
            return getSupportedLanguagesForDetection().contains(srcLang);
        }

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public void setServiceId(String serviceId) {
            // leave empty
        }

        @Override
        public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
            if (languageDetectionObjs.isEmpty()) {
                return;
            }
            String authToken = TranslationApiClient.token.get();
            if (authToken == null) {
                throw new LanguageDetectionException(TOKEN_ERROR_MESSAGE);
            }
            // convert LanguageDetectionObj to LangDetectRequest for the POST request to translation API
            LangDetectRequest langDetectRequest = TranslationClientUtils.createLangDetectRequest(languageDetectionObjs);

            try {
                LangDetectResponse response = getTranslationApiRestClient().getDetectedLanguages(getJsonString(langDetectRequest), authToken);
                List<String> detectedLang = response.getLangs();
                for (int i = 0; i < detectedLang.size(); i++) {
                    languageDetectionObjs.get(i).setDetectedLang(detectedLang.get(i));
                }
            } catch (TranslationApiException e) {
                if (e instanceof ExternalServiceException) {
                    // throw gateway timeout (504) for External service call exceptions.
                    // This will also include the Resource Exhausted Exception
                    throw new LanguageDetectionException(e.getMessage(), HttpStatus.GATEWAY_TIMEOUT.value(), e);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(e.getMessage());
                }
                throw new LanguageDetectionException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
            }
        }

        @Override
        public void close() {
            TranslationApiClient.this.close();
        }

        @Override
        public String getExternalServiceEndPoint() {
            return null;
        }
    }


    // Translation client
    private class TranslationClient implements TranslationService {

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public void setServiceId(String serviceId) {
            // leave empty
        }

        @Override
        public boolean isSupported(String srcLang, String trgLang) {
            return getSupportedLanguagesForTranslation().contains(new LanguagePair(srcLang, trgLang));
        }

        @Override
        public void translate(List<TranslationObj> translationStrings) throws TranslationException {
            if (translationStrings.isEmpty()) {
                return;
            }
            String authToken = TranslationApiClient.token.get();
            if (authToken == null) {
                throw new TranslationException(TOKEN_ERROR_MESSAGE);
            }
            // convert TranslationObj to TranslationRequest for the POST request to translation API
            TranslationRequest translationRequest = TranslationClientUtils.createTranslationRequest(translationStrings);

            try {
                TranslationResponse response = getTranslationApiRestClient().getTranslations(getJsonString(translationRequest), authToken);
                List<String> translations = response.getTranslations();
                for (int i = 0; i < translations.size(); i++) {
                    translationStrings.get(i).setTranslation(translations.get(i));
                }

            } catch (TranslationApiException e) {
                if (e instanceof ExternalServiceException) {
                    // throw gateway timeout (504) for External service call exceptions.
                    // This will also include the Resource Exhausted Exception
                    throw new TranslationException(e.getMessage(), HttpStatus.GATEWAY_TIMEOUT.value(), e);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(e.getMessage());
                }
                throw new TranslationException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
            }

        }

        @Override
        public void close() {
            TranslationApiClient.this.close();
        }

        @Override
        public String getExternalServiceEndPoint() {
            return null;
        }

    }

    private <T> String getJsonString(T request) throws TranslationApiException {
        try {
            return getObjectWriter().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new TranslationApiException("Error parsing the request for Translation API");
        }
    }
}
