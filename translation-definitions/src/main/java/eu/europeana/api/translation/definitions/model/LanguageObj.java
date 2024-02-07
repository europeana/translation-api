package eu.europeana.api.translation.definitions.model;

/**
 * Interface with shared methods
 * @author srishti singh
 * @since 5 Feb 2024
 */
 public interface LanguageObj {

     String getText();

     void setText(String text);

     String getCacheKey();

     void setCacheKey(String cacheKey);

     boolean isAvailableInCache();

     void setAvailableInCache(boolean cached);

     boolean isTranslatable();
}
