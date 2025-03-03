package eu.europeana.api.translation.service.tika;

import java.util.List;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Dummy implementation returning original text in translations
 */
public class DummyApacheTikaLangDetectWithThresholdsService extends ApacheTikaLangDetectWithThresholdsService {

  /**
   * enable default constructor
   */
  public DummyApacheTikaLangDetectWithThresholdsService() {
    super();
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
