package eu.europeana.api.translation.service.util;

/**
 * Utilities for logging 
 * @author GordeaS
 *
 */
public class LoggingUtils {

  //hide default contructor
  private LoggingUtils() {};
  
  /**
   * Sanitize user input to prevent malicious code
   * @param input
   * @return
   */
  public static String sanitizeUserInput(String input) {
    return input.replaceAll("[\n\r]", "_");
  }
}
