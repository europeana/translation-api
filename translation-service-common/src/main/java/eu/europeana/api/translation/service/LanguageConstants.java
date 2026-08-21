package eu.europeana.api.translation.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Constants used by the Apache Tika language detection service
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public final class LanguageConstants {

  private static final Logger LOGGER = LogManager.getLogger(LanguageConstants.class);
  public static final String CLOSE_LANGUAGES_PROPERTIES_FILE = "/close-languages.properties";

  public static final Map<String, Set<String>> closeLanguages = loadCloseLanguages();

  private static Map<String, Set<String>> loadCloseLanguages() {
    Properties properties = new Properties();
    try (InputStream in = LanguageConstants.class.getResourceAsStream(CLOSE_LANGUAGES_PROPERTIES_FILE)) {
      if (in == null) {
        LOGGER.error("Properties file {} not found on classpath", CLOSE_LANGUAGES_PROPERTIES_FILE);
        throw new IllegalStateException("Properties file " + CLOSE_LANGUAGES_PROPERTIES_FILE + " not found on classpath");
      }
      properties.load(in);
    } catch (IOException e) {
      LOGGER.error("Failed to load properties file {}", CLOSE_LANGUAGES_PROPERTIES_FILE, e);
      throw new IllegalStateException("Failed to load properties file " + CLOSE_LANGUAGES_PROPERTIES_FILE, e);
    }

    Map<String, Set<String>> map = new HashMap<>();
    for (String key : properties.stringPropertyNames()) {
      String value = properties.getProperty(key);
      if (value != null && !value.isBlank()) {
        Set<String> set = Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
        map.put(key.trim(), set);
      } else {
        map.put(key.trim(), Set.of());
      }
    }
    return Map.copyOf(map);
  }
  
  /**
   * No instances
   */
  private LanguageConstants() {
  }
}
