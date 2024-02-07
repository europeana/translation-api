package eu.europeana.api.translation.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europeana.api.translation.client.config.TranslationClientConfiguration;
import eu.europeana.api.translation.client.exception.TranslationApiException;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.LangDetectRequest;
import eu.europeana.api.translation.definitions.model.LangDetectResponse;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;

public class TranslationApiClient extends BaseTranslationApiClient {


    public TranslationApiClient(TranslationClientConfiguration configuration) throws TranslationApiException {
        super(configuration);
    }

    public TranslationResponse translate(TranslationRequest request, String authToken) throws TranslationApiException {
        return getTranslationApiRestClient().getTranslations(getJsonString(request), authToken);
    }


    public LangDetectResponse detectLang(LangDetectRequest langDetectRequest, String authToken) throws TranslationApiException {
        return getTranslationApiRestClient().getDetectedLanguages(getJsonString(langDetectRequest), authToken);

    }

    /**
     * indicates if the given language is supported by the language detection service
     * @param srcLang language hint
     * @return true is supported
     */
    public boolean isSupported(String srcLang) {
        return getSupportedLanguagesForDetection().contains(srcLang);
    }

    /**
     * To validate the given pair of source and target language is valid for translation
     *
     * @param srcLang source language of the data to be translated
     * @param trgLang target language in which data has to be translated
     * @return true is the pair is valid
     */
    public boolean isSupported(String srcLang, String trgLang) {
        return getSupportedLanguagesForTranslation().contains(new LanguagePair(srcLang, trgLang));
    }

    private <T> String getJsonString(T request) throws TranslationApiException {
        try {
            return getObjectWriter().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new TranslationApiException("Error parsing the request for Translation API");
        }
    }
}
