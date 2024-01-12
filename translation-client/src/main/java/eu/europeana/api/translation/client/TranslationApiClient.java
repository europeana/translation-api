package eu.europeana.api.translation.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europeana.api.translation.client.config.TranslationClientConfiguration;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.definitions.model.LangDetectRequest;
import eu.europeana.api.translation.definitions.model.LangDetectResponse;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;

public class TranslationApiClient extends BaseTranslationApiClient {


    public TranslationApiClient(TranslationClientConfiguration configuration) {
        super(configuration);
    }

    public TranslationResponse translate(TranslationRequest request) throws TranslationApiException {
        return getTranslationApiRestClient().getTranslations(getJsonString(request));
    }


    public LangDetectResponse detectLang(LangDetectRequest langDetectRequest) throws TranslationApiException {
        return getTranslationApiRestClient().getDetectedLanguages(getJsonString(langDetectRequest));

    }

    private <T> String getJsonString(T request) throws TranslationApiException {
        try {
            return getObjectWriter().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new TranslationApiException("Error parsing the request for Translation API");
        }
    }
}
