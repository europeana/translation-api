package eu.europeana.api.translation.service.google;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Dummy implementation preventing invocation of remote google service
 * @author GordeaS
 *
 */
public class DummyGLangDetectService extends GoogleLangDetectService {

  /**
   * Constructor using dummy project id
   * @param clientWrapperBean the client wrapper implemnetation
   */
  public DummyGLangDetectService(GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(GoogleTranslationServiceClientWrapper.MOCK_CLIENT_PROJ_ID, clientWrapperBean);
  }
  
  @Override
  public List<String> detectLang(List<String> texts, String langHint)
      throws LanguageDetectionException {
    String value = StringUtils.isNotBlank(langHint) ?  langHint : "en";
    ArrayList<String> ret = new ArrayList<>(); 
    
    for (int i = 0; i < texts.size(); i++) {
      ret.add(value);
    }
    return ret;
  }
}
