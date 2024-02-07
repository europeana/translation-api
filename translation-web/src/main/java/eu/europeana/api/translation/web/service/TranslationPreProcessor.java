package eu.europeana.api.translation.web.service;

import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Pre processing class for the Translation flow
 * @author srishti singh
 * @since 31 Jan 2024
 */
public class TranslationPreProcessor implements TranslationService {

    Pattern translationEligibleValuesPattern;

    /**
     * TranslationPreProcessor constructor to initialise the pattern
     * @param translationEligibleValuesPattern eligible values pattern
     */
    public TranslationPreProcessor(Pattern translationEligibleValuesPattern) {
        this.translationEligibleValuesPattern = translationEligibleValuesPattern;
    }
    @Override
    public String getServiceId() {
        return "TEXT_PROCESSOR";
    }

    @Override
    public void setServiceId(String serviceId) {
    // leave empty
    }

    @Override
    public boolean isSupported(String srcLang, String trgLang) {
        // probably not used for now, but better return true, as it accepts any values
        return true;
    }

    /**
     * Check if the text present is an eligible value.
     * Eligible Value : Any value that has at least 2 unicode consecutive letters.
     * If value is not eligible, set isTranslated as false, which means we will not translate that text/value
     * @param translationStrings
     * @return
     */
    @Override
    public void translate(List<TranslationObj> translationStrings) throws TranslationException {
        for (TranslationObj obj : translationStrings) {
            if (!translationEligibleValuesPattern.matcher(obj.getText()).find()) {
                obj.setIsTranslated(false);
            }
        }
    }

    @Override
    public void close() {
        // leave empty, nothing to close
    }

    @Override
    public String getExternalServiceEndPoint() {
        return null;
    }
}
