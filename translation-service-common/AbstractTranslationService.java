package eu.europeana.api.translation.service.pangeanic;

import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.definitions.language.Language;

public class AbstractTranslationService {

  public static boolean noTranslationRequired(String lang) {
    return (lang == null || StringUtils.equals(lang, Language.NO_LINGUISTIC_CONTENT)
        || StringUtils.equals(lang, Language.ENGLISH));
  }
}
