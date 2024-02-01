package eu.europeana.api.translation.web.service;

import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.web.utils.PreProcessorUtils;

import java.util.List;

public class LangDetectionPreProcessor implements LanguageDetectionService {

    @Override
    public boolean isSupported(String srcLang) {
        return false;
    }

    @Override
    public String getServiceId() {
        return null;
    }

    @Override
    public void setServiceId(String serviceId) {

    }

    @Override
    public List<String> detectLang(List<String> texts, String langHint) throws LanguageDetectionException {
        return PreProcessorUtils.filterEligibleValues(texts);
    }

    @Override
    public void close() {

    }

    @Override
    public String getExternalServiceEndPoint() {
        return null;
    }
}
