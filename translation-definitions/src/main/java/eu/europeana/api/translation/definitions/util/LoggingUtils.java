package eu.europeana.api.translation.definitions.util;

public class LoggingUtils {

  public static String sanitizeUserInput(String input) {
    return input.replaceAll("[\n\r]", "_");
  }
}
