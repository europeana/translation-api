package eu.europeana.api.translation.definitions.model;

/**
 * The Data Model class used for holding information used during the processing of translation requests
 */
public class TranslationObj extends LanguageObj {

  private String sourceLang;
  private String targetLang;
  private String translation;

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
}
