package eu.europeana.api.translation.service.google;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Dummy implementation preventing invocation of remote google service
 * @author GordeaS
 *
 */
public class DummyGLangDetectWithThresholdsService extends GoogleLangDetectWithThresholdService {

  /**
   * Constructor using dummy project id
   * @param clientWrapperBean the client wrapper implemnetation
   */
  public DummyGLangDetectWithThresholdsService(GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(GoogleTranslationServiceClientWrapper.MOCK_CLIENT_PROJ_ID, clientWrapperBean);
  }
  
  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
    String langHint = languageDetectionObjs.get(0).getHint();
    String value = StringUtils.isNotBlank(langHint) ?  langHint : "en";
    for (LanguageDetectionObj obj : languageDetectionObjs) {
      obj.setDetectedLang(value);
    }
  }
}
