package eu.europeana.api.translation.service.google;

import java.util.List;
import eu.europeana.api.translation.definitions.service.exception.TranslationException;

public class DummyGTranslateService extends GoogleTranslationService {

  public DummyGTranslateService(String googleProjectId,
      GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(googleProjectId, clientWrapperBean);
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
