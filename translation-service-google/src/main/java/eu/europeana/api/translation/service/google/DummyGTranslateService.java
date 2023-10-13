package eu.europeana.api.translation.service.google;

import java.util.List;
import eu.europeana.api.translation.definitions.service.exception.TranslationException;

/**
 * Dummy implementation preventing invocation of remote google service
 * @author GordeaS
 *
 */
public class DummyGTranslateService extends GoogleTranslationService {

  /**
   * Constructor using dummy project id
   * @param clientWrapperBean the client wrapper implemnetation
   */
  public DummyGTranslateService(GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(GoogleTranslationServiceClientWrapper.MOCK_CLIENT_PROJ_ID, clientWrapperBean);
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
