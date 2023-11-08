package eu.europeana.api.translation.service.pangeanic;

import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.definitions.language.Language;
import eu.europeana.api.translation.service.TranslationService;

/**
 * Base abstract class to be used by translation services 
 * @author GordeaS
 *
 */
public abstract class AbstractTranslationService implements TranslationService{

  /**
   * @deprecated the verification of supported languages is part of the api request validation
   * utility method to verify if the translation is required for the given language 
   * @param lang the language of the text
   * @return true if translation not needed
   */
  @Deprecated
  public static boolean noTranslationRequired(String lang) {
    return (lang == null || StringUtils.equals(lang, Language.NO_LINGUISTIC_CONTENT)
        || StringUtils.equals(lang, Language.ENGLISH));
  }
}
