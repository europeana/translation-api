package eu.europeana.api.translation.service.google;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.definitions.service.exception.LanguageDetectionException;

public class DummyGLangDetectService extends GoogleLangDetectService {

  public DummyGLangDetectService(String googleProjectId,
      GoogleTranslationServiceClientWrapper clientWrapperBean) {
    super(googleProjectId, clientWrapperBean);
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
