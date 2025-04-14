package eu.europeana.api.translation.web.service;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pre processing class for the Language detection flow
 * @author srishti singh
 * @since 31 Jan 2024
 */
public class LangDetectionPreProcessor implements LanguageDetectionService {

    Pattern langDetectEligibleValuesPattern;

    /**
     * LangDetectionPreProcessor constructor to initialise the pattern
     * @param langDetectEligibleValuesPattern eligible values pattern
     */
    public LangDetectionPreProcessor(Pattern langDetectEligibleValuesPattern) {
        this.langDetectEligibleValuesPattern = langDetectEligibleValuesPattern;
    }

    @Override
    public boolean isSupported(String srcLang) {
        // probably not used for now, but better return true, as it accepts any values
        return true;
    }

    @Override
    public String getServiceId() {
        return "TEXT_PROCESSOR";
    }

    @Override
    public void setServiceId(String serviceId) {
        // leave empty
    }

    /**
     * Check if the text present is an eligible value.
     * Eligible Value : Any value that has at least 2 unicode consecutive letters.
     * If value is not eligible, set isTranslated to true.
     * @param languageDetectionObjs languge detection input object list
     * @throws LanguageDetectionException
     */
    @Override
    public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
        for (LanguageDetectionObj obj : languageDetectionObjs) {
            if (!langDetectEligibleValuesPattern.matcher(obj.getText()).find()) {
                obj.setTranslated(true);
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
