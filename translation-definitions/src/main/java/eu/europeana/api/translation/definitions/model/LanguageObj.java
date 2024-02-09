package eu.europeana.api.translation.definitions.model;

/**
 * Interface with shared methods
 * @author srishti singh
 * @since 5 Feb 2024
 */
 public abstract class LanguageObj {

    private String text;
    private String detectedLang;
    private String cacheKey;
    private boolean retrievedFromCache;

    /**
     * Set to :
     *      true - if during the pre-processing or other workflow
     *             that value is already translated
     *
     *     false - when the value is yet not translated and is yet to be
     *             sent to the services for translations
     */
    private boolean isTranslated;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDetectedLang() {
        return detectedLang;
    }

    public void setDetectedLang(String detectedLang) {
        this.detectedLang = detectedLang;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public boolean isRetrievedFromCache() {
        return retrievedFromCache;
    }

    public void setRetrievedFromCache(boolean retrievedFromCache) {
        this.retrievedFromCache = retrievedFromCache;
    }

    public boolean isTranslated() {
        return isTranslated;
    }

    public void setTranslated(boolean translated) {
        isTranslated = translated;
    }
}
