package eu.europeana.api.translation.service.tika;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.AbstractLanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

/**
 * Apache Tika language detection service
 *
 * @author Srdjan
 */
public class ApacheTikaLangDetectService extends AbstractLanguageDetectionService{

  private final LanguageDetector detector;
  private Set<String> expectedLanguages;
  
  /**
   * Default constructor
   */
  public ApacheTikaLangDetectService() {
    this.detector = new OptimaizeLangDetector().loadModels();
  }

  @Override
  public void setThresholdsConf(ThresholdsConfiguration thresholdsConf) throws LangDetectionServiceConfigurationException {
    super.setThresholdsConf(thresholdsConf);
    if (getThresholdsConf() != null && getThresholdsConf().getNonSupportedLangPrior()!=null) {
      
      HashMap<String, Float> defaultPriors=new HashMap<String, Float>(ApacheTikaConstants.supportedLanguages.size());
      for(String  lang: ApacheTikaConstants.supportedLanguages) {
        if(detector.hasModel(lang)) {
          if(getExpectedLanguages() != null && getExpectedLanguages().contains(lang)) {
            defaultPriors.put(lang, 1f);
          } else {
            defaultPriors.put(lang, thresholdsConf.getNonSupportedLangPrior());
          }
        }
      }
      try {
        detector.setPriors(defaultPriors);
      } catch (IOException e) {
        throw new LangDetectionServiceConfigurationException(
            "Error setting Tika's language priors", e);
      }
    }
  }

  @Override
  public boolean isSupported(String srcLang) {
    return ApacheTikaConstants.supportedLanguages.contains(srcLang);
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs)
      throws LanguageDetectionException {
    if (languageDetectionObjs.isEmpty()) {
      return;
    }

    List<String> detectedLangs = new ArrayList<>(languageDetectionObjs.size());
    List<LanguageResult> tikaLanguages = null;
    for (LanguageDetectionObj obj : languageDetectionObjs) {
      // returns all tika languages sorted by score
      tikaLanguages = this.detector.detectAll(obj.getText());
      detectedLangs.add(chooseDetectedLang(obj.getText(), tikaLanguages, obj.getHint()));
    }

    // fallback check - if the lang detection is complete / successful
    if (detectedLangs.size() != languageDetectionObjs.size()) {
      throw new LanguageDetectionException(
          "The Language detection is not completed successfully. Expected "
              + languageDetectionObjs.size() + " but received: " + detectedLangs.size());
    }
    // build results
    for (int i = 0; i < detectedLangs.size(); i++) {
      languageDetectionObjs.get(i).setDetectedLang(detectedLangs.get(i));
    }
  }

  protected String chooseDetectedLang(String sourceText, List<LanguageResult> tikaLanguages,
      String langHint) {
    if (tikaLanguages.isEmpty())
      return null;
    if (getThresholdsConf() != null) {
      return chooseDetectedLangUsingThresholds(sourceText, tikaLanguages, langHint);
    }

    // In case lang hint is not null, check if it myabe exists among the langs with
    // the highest confidence, and if so return the langHint as a detected lang, if
    // not return the first one.
    // if langHint is null, return the first detected language (has the highest
    // confidence)
    String detectedLang = tikaLanguages.get(0).getLanguage();
    if (StringUtils.isBlank(langHint)) {
      return detectedLang;
    } else if (langHint.equals(detectedLang) || containsHint(tikaLanguages, langHint)) {
      // search hint above confidence level
      detectedLang = langHint;
    }
    return detectedLang;
  }

  boolean containsHint(List<LanguageResult> tikaLanguages, String langHint) {
    if (langHint == null) {
      return false;
    }
    boolean ret = false;
    //enable when  float confidence = tikaLanguages.get(0).getRawScore();
    for (int i = 1; i < tikaLanguages.size(); i++) {
      if (langHint.equals(tikaLanguages.get(i).getLanguage())) {
        ret = true;
        break;
      }
    }
    return ret;
  }

  protected String chooseDetectedLangUsingThresholds(String sourceText,
      List<LanguageResult> tikaLanguages, String langHint) {
    //
    String detectedLang = tikaLanguages.get(0).getLanguage();
    float confidence = tikaLanguages.get(0).getRawScore();
    if (getThresholdsConf().isAcceptableDetection(sourceText, langHint, confidence))
      return detectedLang;
    else
      return StringUtils.isBlank(langHint) ? null : langHint;
  }

  @Override
  public void close() {
    // nothing to do
  }

  public Set<String> getExpectedLanguages() {
    return expectedLanguages;
  }

  public void setExpectedLanguages(Set<String> expectedLanguages) {
    this.expectedLanguages = expectedLanguages;
  }


}
