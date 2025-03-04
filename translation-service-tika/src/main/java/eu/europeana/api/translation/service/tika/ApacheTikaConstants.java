package eu.europeana.api.translation.service.tika;

import java.util.Set;

/**
 * Constants used by the Apache Tika language detection service
 * 
 * @author Nuno Freire
 * @since 29/01/2025
 */
public class ApacheTikaConstants {

  protected static final Set<String> supportedLanguages = Set.of("af", "an", "ar", "ast", "be", "br", "ca", "bg", "bn",
      "cs", "cy", "da", "de", "el", "en", "es", "et", "eu", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi", "hr", "ht",
      "hu", "id", "is", "it", "ja", "km", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ms", "mt", "ne", "nl", "no", "oc",
      "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur",
      "vi", "wa", "yi", "zh-cn", "zh-tw");
}
