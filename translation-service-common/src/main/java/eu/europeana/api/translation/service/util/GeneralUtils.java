package eu.europeana.api.translation.service.util;

import org.apache.commons.lang3.StringUtils;

public class GeneralUtils {

  /**
   * Splitting a string into an array. A null separator splits on whitespace.
   * @param concatenatedStrings
   * @param separator
   * @return
   */
  public static String[] toArray(String concatenatedStrings, String separator) {
      if (StringUtils.isEmpty(concatenatedStrings))
          return null;
      String[] array = StringUtils.splitByWholeSeparator(concatenatedStrings, separator);
      return StringUtils.stripAll(array);
  }   

}
