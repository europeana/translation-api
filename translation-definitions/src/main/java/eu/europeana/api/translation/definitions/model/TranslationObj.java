package eu.europeana.api.translation.definitions.model;

/**
 * The Data Model class used for holding information used during the processing of translation requests  
 */
public class TranslationObj {
  private String text;
  private String sourceLang;
  private String targetLang;
  private String translation;
  private String cacheKey;
  private boolean isCached;

  public String getText() {
    return text;
  }
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
  public String getCacheKey() {
    return cacheKey;
  }
  public void setCacheKey(String cacheKey) {
    this.cacheKey = cacheKey;
  }
  
  @SuppressWarnings({"java:S1309", "PMD.BooleanGetMethodName"})
  public boolean getIsCached() {
    return isCached;
  }
  public void setIsCached(boolean cached) {
    this.isCached = cached;
  }

}
