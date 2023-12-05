package eu.europeana.api.translation.service.pangeanic;

import java.util.List;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;

/**
 * Dummy implementation preventing invocation of remote pangeanic service
 * @author GordeaS
 *
 */
public class DummyPangTranslationService extends PangeanicTranslationService{

  /**
   * Constructor using null as endpoint
   * @throws TranslationServiceConfigurationException 
   */
  public DummyPangTranslationService() throws TranslationServiceConfigurationException {
    super(null, null, null);
  }
  
  @Override
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    for(TranslationObj obj : translationObjs) {
      obj.setTranslation(obj.getText());
    }
  }

}
