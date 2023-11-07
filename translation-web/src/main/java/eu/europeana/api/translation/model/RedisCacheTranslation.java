package eu.europeana.api.translation.model;

public class RedisCacheTranslation {
  private String original;
  private String translation;
  public String getOriginal() {
    return original;
  }
  public void setOriginal(String original) {
    this.original = original;
  }
  public String getTranslation() {
    return translation;
  }
  public void setTranslation(String translation) {
    this.translation = translation;
  }
}
