package eu.europeana.api.translation.definitions.model;

/**
 * The class for storing the translation and caching statistics
 */
public class TranslationCachingStats {

  private int numLinesCached;
  private int numCharsCached;
  private int numLinesToBeTranslated;
  private int numCharsToBeTranslated;
  
  public int getNumLinesCached() {
    return numLinesCached;
  }
  public void setNumLinesCached(int numLinesCached) {
    this.numLinesCached = numLinesCached;
  }
  public int getNumCharsCached() {
    return numCharsCached;
  }
  public void setNumCharsCached(int numCharsCached) {
    this.numCharsCached = numCharsCached;
  }
  public int getNumLinesToBeTranslated() {
    return numLinesToBeTranslated;
  }
  public void setNumLinesToBeTranslated(int numLinesToBeTranslated) {
    this.numLinesToBeTranslated = numLinesToBeTranslated;
  }
  public int getNumCharsToBeTranslated() {
    return numCharsToBeTranslated;
  }
  public void setNumCharsToBeTranslated(int numCharsToBeTranslated) {
    this.numCharsToBeTranslated = numCharsToBeTranslated;
  }
  
}
