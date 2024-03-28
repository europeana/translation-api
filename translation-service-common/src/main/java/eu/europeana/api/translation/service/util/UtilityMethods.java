package eu.europeana.api.translation.service.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.google.common.primitives.Ints;

public class UtilityMethods {
  
  private UtilityMethods() {
  }

  /**
   * generate redis keys
   * 
   * @param inputText the original text
   * @param sourceLang language of the original text
   * @param targetLang language of the translation
   * @param forETransl if key id used as eTranslation reference
   * @return generated redis key
   */
  public static String generateRedisKey(String inputText, String sourceLang, String targetLang, boolean forETransl) {
    StringBuilder builder = new StringBuilder();
    if(forETransl) {
      builder.append("et:");
    }
    builder.append(sourceLang).append(targetLang);
    byte[] hash =
        Base64.getEncoder().withoutPadding().encode(Ints.toByteArray(inputText.hashCode()));
    builder.append(new String(hash, StandardCharsets.UTF_8));
    return builder.toString();
  }
}
