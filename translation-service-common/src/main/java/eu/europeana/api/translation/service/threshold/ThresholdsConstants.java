package eu.europeana.api.translation.service.threshold;

/**
 * Constants used by the Tika and Google language detection services
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class ThresholdsConstants {
  // JSON config field names
  public static final String HINT_THRESHOLDS = "hintThresholds";
  public static final String NO_HINT_THRESHOLDS = "noHintThresholds";
  public static final String MIN_LENGTH = "minLength";
  public static final String MAX_LENGTH = "maxLength";
  public static final String CONFIDENCE_THRESHOLD = "confidenceThreshold";
  public static final String NON_SUPPORTED_LANG_PRIOR = "nonSupportedLangPrior";

  
  /**
   * No instances
   */
  private ThresholdsConstants() {
  }
}
