package eu.europeana.api.translation.definitions.model;

/**
 * Interface with shared methods
 * @author srishti singh
 * @since 5 Feb 2024
 */
public interface LanguageObj {

    public String getText();

    public void setText(String text);

    public String getCacheKey();

    public void setCacheKey(String cacheKey);

    public boolean isAvailableInCache();

    public void setAvailableInCache(boolean cached);

    public boolean isTranslatable();
}
