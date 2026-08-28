package eu.europeana.api.translation.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Constants used by the Apache Tika language detection service
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class RelatedLanguages{

  public static final String RELATED_LANGUAGES_CONFIG_FILE = "/related-languages.properties";

  private final Map<String, Set<String>> relatedLanguagesMap;
 
  /**
   * default constructor with no close languages
   */
  public RelatedLanguages() {
    //initialize with empty map, allowing the use of addCloseLanguages
    this.relatedLanguagesMap =  new ConcurrentHashMap<>();
  }
  
  /**
   * Constructor with related languages provided as properties
   * @param relatedLanguagesProps related languages provided as properties
   */
  public RelatedLanguages(Properties relatedLanguagesProps) {
    if(relatedLanguagesProps == null || relatedLanguagesProps.isEmpty()) {
      this.relatedLanguagesMap =  new ConcurrentHashMap<>();
    } else {
      this.relatedLanguagesMap = parseRelatedLanguages(relatedLanguagesProps);  
    }
  }
   
  /**
   * Constructor with related languages provided as map
   * @param relatedLanguagesMap
   */
  public RelatedLanguages(Map<String, Set<String>> relatedLanguagesMap) {
    if(relatedLanguagesMap == null) {
      this.relatedLanguagesMap =  new ConcurrentHashMap<>();
    } else {
      this.relatedLanguagesMap = Map.copyOf(relatedLanguagesMap);   
    }
  }
  
  
  /**
   * Provides the set of related languages
   * @param mainLanguage main language in the family
   * @return set of related languages (ISO Code )
   */
  public Set<String> getCloseLanguages(String mainLanguage) {
    if(relatedLanguagesMap == null || !relatedLanguagesMap.containsKey(mainLanguage)) {
      return Collections.emptySet();
    }
    return relatedLanguagesMap.get(mainLanguage);
  }
  
  
  /**
   * Method to add related languages 
   * @param relatedLanguagesProps related languages provided as properties
   */
  public void addCloseLanguages(Properties relatedLanguagesProps) {
    this.relatedLanguagesMap.putAll(parseRelatedLanguages(relatedLanguagesProps));
  }
  
  /**
   * Parse the related languages from properties to map
   * @param properties related languages as props
   * @return the map of related languages
   */
  private Map<String, Set<String>> parseRelatedLanguages(Properties properties) {
    Map<String, Set<String>> map = new ConcurrentHashMap<>();
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
    return map;
  }
  
}
