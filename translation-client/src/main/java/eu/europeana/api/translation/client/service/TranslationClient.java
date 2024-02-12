package eu.europeana.api.translation.client.service;

import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

import java.util.ArrayList;
import java.util.List;

public class TranslationClient implements TranslationService {

    @Override
    public String getServiceId() {
        return "TRANSLATION_CLIENT";
    }

    @Override
    public void setServiceId(String serviceId) {
        // leave empty
    }

    @Override
    public boolean isSupported(String srcLang, String trgLang) {
        return false;
    }

    @Override
    public void translate(List<TranslationObj> translationStrings) throws TranslationException {
        if (translationStrings.isEmpty()) {
            return;
        }
        // convert TranslationObj to TranslationRequest for the POST request to translation API
        TranslationRequest translationRequest = new TranslationRequest();
        translationRequest.setSource(translationStrings.get(0).getSourceLang());
        translationRequest.setTarget(translationStrings.get(0).getTargetLang());

        List<String> text = new ArrayList<>();
        for (TranslationObj object : translationStrings) {
            text.add(object.getText());
        }
        translationRequest.setText(text);

//        getTranslationApiRestClient().getTranslations(getJsonString(translationRequest), authToken);
       // return getTranslationApiRestClient().getDetectedLanguages(getJsonString(langDetectRequest), authToken);


    }

    @Override
    public void close() {
        // leave empty
    }

    @Override
    public String getExternalServiceEndPoint() {
        return null;
    }

}
