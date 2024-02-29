package eu.europeana.api.translation.definitions.model;

/**
 * The Data Model class used for holding information used during the processing of Language Detection requests
 * @author srishti singh
 * @since 6 Feb 2024
 */
public class LanguageDetectionObj extends LanguageObj {

    private String hint;
    private String detectedLang;

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getDetectedLang() {
        return detectedLang;
    }

    public void setDetectedLang(String detectedLang) {
        this.detectedLang = detectedLang;
    }
}
