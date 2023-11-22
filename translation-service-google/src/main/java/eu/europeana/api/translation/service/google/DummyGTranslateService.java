package eu.europeana.api.translation.service.google;

import java.util.List;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.exception.TranslationException;

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
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    for(TranslationObj obj : translationObjs) {
      obj.setTranslation(obj.getText());
    }
  }

}
