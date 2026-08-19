package eu.europeana.api.translation.service.tika;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.AbstractLanguageDetectionService;
import eu.europeana.api.translation.service.LanguageConstants;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
 
/**
 * Apache Tika language detection service
 *
 * @author Srdjan
 */
public class ApacheTikaLangDetectService extends AbstractLanguageDetectionService {

  private final static Logger logger = LogManager.getLogger(AbstractLanguageDetectionService.class);
  
  //Using a ThreadLocal for the Tika detector for thread-safe use
  private final ThreadLocal<LanguageDetector> detector = ThreadLocal.withInitial(this::createDetector); 
  //The list of languages that are likely to be in the text
  private Set<String> expectedLanguages;
  //The probability priors for Tika, assigning a higher probability for languages in the list of expectedLanguages
  private Map<String, Float> priors;

  /**
   * Default constructor
   */
  public ApacheTikaLangDetectService() {
  }

  /**
   * Set detector's priors based on service configurations for API's supported
   * languages
   * 
   * @param expectedLanguages the languages supported by the API
   * @throws LangDetectionServiceConfigurationException if the priori cannot be
   *                                                    set
   */
  public void initDetectorPriors(List<String> expectedLanguages) throws LangDetectionServiceConfigurationException {
    setExpectedLanguages(expectedLanguages);
    if (getThresholdsConf() == null || getThresholdsConf().getNonSupportedLangPrior() == null) {
      // no config for unsupported language priors
      return;
    }

    LanguageDetector tmpDetector = new OptimaizeLangDetector().loadModels();
    
    // build priors
    priors = HashMap.newHashMap(ApacheTikaConstants.supportedLanguages.size());
    for (String lang : ApacheTikaConstants.supportedLanguages) {
      // only if model available
      if (!tmpDetector.hasModel(lang)) {
        continue;
      }

      // add prior
      addPrior(lang, priors);
    }
  }

  private void addPrior(String lang, Map<String, Float> defaultPriors) {
    if (getExpectedLanguages() != null && getExpectedLanguages().contains(lang)) {
      defaultPriors.put(lang, 1F);
    } else {
      defaultPriors.put(lang, getThresholdsConf().getNonSupportedLangPrior());
    }
  }
  
  private LanguageDetector createDetector(){
    OptimaizeLangDetector detectorInstance = new OptimaizeLangDetector();
    if(priors!=null) {
      try {
          detectorInstance.setPriors(priors);
      } catch (IOException e) {
        logger.warn("Could not initialize the priors for Tika language detector service", e);  
      }
    }
    detectorInstance.loadModels();
    return detectorInstance;
}

  @Override
  public boolean isSupported(String srcLang) {
    return ApacheTikaConstants.supportedLanguages.contains(srcLang);
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
    if (languageDetectionObjs.isEmpty()) {
      return;
    }

    List<String> detectedLangs = new ArrayList<>(languageDetectionObjs.size());
    List<LanguageResult> tikaLanguages = null;
    for (LanguageDetectionObj obj : languageDetectionObjs) {
      // returns all tika languages sorted by score
      tikaLanguages = this.detector.get().detectAll(obj.getText());
      detectedLangs.add(chooseDetectedLang(obj.getText(), tikaLanguages, obj.getHint()));
    }

    // fallback check - if the lang detection is complete / successful
    if (detectedLangs.size() != languageDetectionObjs.size()) {
      throw new LanguageDetectionException("The Language detection is not completed successfully. Expected "
          + languageDetectionObjs.size() + " but received: " + detectedLangs.size());
    }
    // build results
    for (int i = 0; i < detectedLangs.size(); i++) {
      languageDetectionObjs.get(i).setDetectedLang(detectedLangs.get(i));
    }
  }

  protected String chooseDetectedLang(String sourceText, List<LanguageResult> tikaLanguages, String langHint) {
    if (tikaLanguages.isEmpty())
      return null;
    if (getThresholdsConf() != null) {
      return chooseDetectedLangUsingThresholds(sourceText, tikaLanguages, langHint);
    }

    // In case lang hint is not null, check if it exists among the langs with
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
    } else {
      detectedLang = overideRegionalLanguage(detectedLang, langHint);
    }
    return detectedLang;
  }

  /**
   * @param detectedLang check if the detected language is a close language to the
   *                     hint. Use the hint in such cases
   * @return
   */
  private String overideRegionalLanguage(String detectedLang, String langHint) {
    if (langHint != null) {
      Set<String> closeLangs = LanguageConstants.closeLanguages.get(langHint);
      if (closeLangs != null && closeLangs.contains(detectedLang))
        detectedLang = langHint;
    }
    return detectedLang;
  }

  boolean containsHint(List<LanguageResult> tikaLanguages, String langHint) {
    if (langHint == null) {
      return false;
    }
    boolean ret = false;
    // enable when float confidence = tikaLanguages.get(0).getRawScore();
    for (int i = 1; i < tikaLanguages.size(); i++) {
      if (langHint.equals(tikaLanguages.get(i).getLanguage())) {
        ret = true;
        break;
      }
    }
    return ret;
  }

  protected String chooseDetectedLangUsingThresholds(String sourceText, List<LanguageResult> tikaLanguages,
      String langHint) {
    //
    if (containsHint(tikaLanguages, langHint))
      return langHint;
    float confidence = tikaLanguages.get(0).getRawScore();
    if (getThresholdsConf().isAcceptableDetection(sourceText, langHint, confidence)) {
      String detectedLang = tikaLanguages.get(0).getLanguage();
      if (!StringUtils.isBlank(langHint) && !langHint.equals(detectedLang))
        detectedLang = overideRegionalLanguage(detectedLang, langHint);
      return detectedLang;
    } else
      return StringUtils.isBlank(langHint) ? null : langHint;
  }

  @Override
  public void close() {
    detector.remove();
  }

  public Set<String> getExpectedLanguages() {
    return expectedLanguages;
  }

  public void setExpectedLanguages(List<String> expectedLanguages) {
    this.expectedLanguages = Set.copyOf(expectedLanguages);
  }

}
