package eu.europeana.api.translation.service.google;

public class DummyGLangDetectService extends GoogleLangDetectService {

  public DummyGLangDetectService(String googleProjectId,
      GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(googleProjectId, clientWrapperBean);
  }
}
