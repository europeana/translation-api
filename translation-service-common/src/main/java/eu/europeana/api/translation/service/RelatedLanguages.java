package eu.europeana.api.translation.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Constants used by the Apache Tika language detection service
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class RelatedLanguages {

  public static final String RELATED_LANGUAGES_CONFIG_FILE = "/related-languages.properties";

  private Map<String, Set<String>> relatedLanguages;
 

  private  Map<String, Set<String>> parseRelatedLanguages(Properties properties) {
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
    return map;
  }
  
  /**
   * No instances
   */
  public RelatedLanguages() {
    //initialize with empty map, allowing the use of addCloseLanguages
    this.relatedLanguages =  new HashMap<>();
  }
  
  /**
   * No instances
   */
  public RelatedLanguages(Properties relatedLanguagesProps) {
    if(relatedLanguagesProps == null || relatedLanguagesProps.isEmpty()) {
      this.relatedLanguages =  new HashMap<>();
    } else {
      this.relatedLanguages = parseRelatedLanguages(relatedLanguagesProps);  
    }
  }
   
  public RelatedLanguages(Map<String, Set<String>> relatedLanguagesMap) {
    if(relatedLanguagesMap == null) {
      this.relatedLanguages =  new HashMap<>();
    } else {
      this.relatedLanguages = Map.copyOf(relatedLanguagesMap);   
    }
  }
  
  
  /**
   * Provides the set of 
   * @param mainLanguage main language in the family
   * @return set of related languages (ISO Code )
   */
  public Set<String> getCloseLanguages(String mainLanguage) {
    if(relatedLanguages == null || !relatedLanguages.containsKey(mainLanguage)) {
      return Collections.emptySet();
    }
    return relatedLanguages.get(mainLanguage);
  }
  
  
  public void addCloseLanguages(Properties relatedLanguagesProps) {
    this.relatedLanguages.putAll(parseRelatedLanguages(relatedLanguagesProps));
  }
}
