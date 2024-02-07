package eu.europeana.api.translation.definitions.model;

/**
 * The Data Model class used for holding information used during the processing of Language Detection requests
 * @author srishti singh
 * @since 6 Feb 2024
 */
public class LanguageDetectionObj implements LanguageObj {

    private String text;
    private String hint;
    private String detectedLang;
    private String cacheKey;
    private boolean availableInCache;
    private boolean isTranslated;


    @Override
    public String getText() {
        return text;
    }

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

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getCacheKey() {
        return cacheKey;
    }

    @Override
    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    @Override
    public boolean isAvailableInCache() {
        return availableInCache;
    }

    @Override
    public void setAvailableInCache(boolean cached) {
        this.availableInCache = cached;
    }

    @Override
    public boolean isTranslatable() {
        return isTranslated;
    }

    public void setIsTranslated(boolean translated) {
        isTranslated = translated;
    }
}
