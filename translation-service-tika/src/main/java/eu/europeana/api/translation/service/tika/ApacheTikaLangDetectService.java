package eu.europeana.api.translation.service.tika;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.language.detect.LanguageResult;

import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;

public class ApacheTikaLangDetectService extends BaseApacheTikaLangDetectService {

  protected static final Logger LOG = LogManager.getLogger(ApacheTikaLangDetectService.class);
  
  /**
   * Default constructor
   */
  public ApacheTikaLangDetectService() {
    super();
  }

  /**
   * In case lang hint is not null, check if it myabe exists among the langs with
   * the highest confidence, and if so return the langHint as a detected lang, if
   * not return the first one.
   */
  protected String chooseDetectedLang(String sourceText, List<LanguageResult> tikaLanguages, String langHint) {
    if (tikaLanguages.isEmpty()) {
      return null;
    }
    // if langHint is null, return the first detected language (has the highest
    // confidence)
    if (StringUtils.isBlank(langHint)) {
      return tikaLanguages.get(0).getLanguage();
    }

    String detectedLang = tikaLanguages.get(0).getLanguage();
    if (langHint.equals(detectedLang)) {
      return langHint;
    }
    float confidence = tikaLanguages.get(0).getRawScore();
    for (int i = 1; i < tikaLanguages.size(); i++) {
      if (tikaLanguages.get(i).getRawScore() >= confidence) {
        if (langHint.equals(tikaLanguages.get(i).getLanguage())) {
          detectedLang = langHint;
          break;
        }
      } else {
        break;
      }
    }
    return detectedLang;
  }


  @Override
  public void setConfiguration(Map<String, LanguageDetectionService> detectionServices, String configResourceName)
      throws LangDetectionServiceConfigurationException {
    //nothing to do
  }
}
