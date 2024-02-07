package eu.europeana.api.translation.definitions.model;

/**
 * The Data Model class used for holding information used during the processing of translation requests  
 */
public class TranslationObj implements LanguageObj{
  private String text;
  private String sourceLang;
  private String targetLang;
  private String translation;
  private String cacheKey;
  private boolean availableInCache;
  private boolean isTranslated;

  @Override
  public String getText() {
    return text;
  }

  @Override
  public void setText(String text) {
    this.text = text;
  }

  public String getSourceLang() {
    return sourceLang;
  }

  public void setSourceLang(String sourceLang) {
    this.sourceLang = sourceLang;
  }

  public String getTargetLang() {
    return targetLang;
  }

  public void setTargetLang(String targetLang) {
    this.targetLang = targetLang;
  }

  public String getTranslation() {
    return translation;
  }

  public void setTranslation(String translation) {
    this.translation = translation;
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
