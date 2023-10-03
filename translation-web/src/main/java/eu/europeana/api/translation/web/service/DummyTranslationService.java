package eu.europeana.api.translation.web.service;

import java.util.List;
import eu.europeana.api.translation.definitions.service.TranslationService;
import eu.europeana.api.translation.definitions.service.exception.TranslationException;

public class DummyTranslationService implements TranslationService {

  public DummyTranslationService() {
  }

  @Override
  public boolean isSupported(String srcLang, String targetLanguage) {
    return true;
  }

  @Override
  public List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage)
      throws TranslationException {
    return texts;
  }

  @Override
  public List<String> translate(List<String> texts, String targetLanguage)
      throws TranslationException {
    return translate(texts, targetLanguage, null);
  }

  @Override
  public void close() {
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

  @Override
  public String getServiceId() {
    return "DUMMY";
  }

  @Override
  public void setServiceId(String serviceId) {
  }

}
