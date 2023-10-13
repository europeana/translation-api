package eu.europeana.api.translation.service.pangeanic;

import java.util.List;
import eu.europeana.api.translation.definitions.service.exception.TranslationException;

/**
 * Dummy implementation preventing invocation of remote pangeanic service
 * @author GordeaS
 *
 */
public class DummyPangTranslationService extends PangeanicTranslationService{

  /**
   * Constructor using null as endpoint
   */
  public DummyPangTranslationService() {
    super(null, null);
  }

  @Override
  public List<String> translate(List<String> text, String targetLanguage, String sourceLanguage)
      throws TranslationException {
    return translate(text, null);
  }
  
  @Override
  public List<String> translate(List<String> text, String targetLanguage)
      throws TranslationException {
    return text;
  }
}