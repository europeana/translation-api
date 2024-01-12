package eu.europeana.api.translation.service.google;

import java.util.List;
import eu.europeana.api.translation.definitions.model.TranslationString;

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
  public void translate(List<TranslationString> translationObjs) {
    for(TranslationString obj : translationObjs) {
      obj.setTranslation(obj.getText());
    }
  }

}
