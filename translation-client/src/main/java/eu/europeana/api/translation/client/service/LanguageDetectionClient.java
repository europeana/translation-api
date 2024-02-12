package eu.europeana.api.translation.client.service;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

import java.util.List;

public class LanguageDetectionClient implements LanguageDetectionService {
    @Override
    public boolean isSupported(String srcLang) {
        return false;
    }

    @Override
    public String getServiceId() {
        return "TRANSLATION_CLIENT";
    }

    @Override
    public void setServiceId(String serviceId) {

    }

    @Override
    public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {

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
