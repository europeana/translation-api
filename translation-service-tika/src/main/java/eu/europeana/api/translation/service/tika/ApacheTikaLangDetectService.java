package eu.europeana.api.translation.service.tika;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

/**
 * Apache Tika language detection service
 *
 * @author Srdjan
 */
public class ApacheTikaLangDetectService implements LanguageDetectionService {

  protected static final Logger LOG = LogManager.getLogger(ApacheTikaLangDetectService.class);
  
  private final LanguageDetector detector;
  private String serviceId;
  private ThresholdsConfiguration thresholdsConf;

  /**
   * Default constructor
   */
  public ApacheTikaLangDetectService() {
    this.detector = new OptimaizeLangDetector().loadModels();
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
      tikaLanguages = this.detector.detectAll(obj.getText());

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

  @Override
  public String getServiceId() {
    return serviceId;
  }

  @Override
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }


  protected String chooseDetectedLang(String sourceText, List<LanguageResult> tikaLanguages, String langHint) {
    if (tikaLanguages.isEmpty())
      return null;
    if (thresholdsConf!=null) 
      return chooseDetectedLangUsingThresholds(sourceText, tikaLanguages, langHint);
      
    //In case lang hint is not null, check if it myabe exists among the langs with
    //the highest confidence, and if so return the langHint as a detected lang, if
    //not return the first one.
    // if langHint is null, return the first detected language (has the highest
    // confidence)
    String detectedLang = tikaLanguages.get(0).getLanguage();
    if (StringUtils.isBlank(langHint)) 
      return detectedLang;
    if (langHint.equals(detectedLang)) 
      return langHint;
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

  protected String chooseDetectedLangUsingThresholds(String sourceText, List<LanguageResult> tikaLanguages, String langHint) {
    //
    String detectedLang = tikaLanguages.get(0).getLanguage();
    float confidence = tikaLanguages.get(0).getRawScore();
    if (thresholdsConf.isAcceptableDetection(sourceText, langHint, confidence))
      return detectedLang;
    else
      return StringUtils.isBlank(langHint) ? null : langHint;    
  }

  @Override
  public void close() {
    // nothing to do
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }


  /**
   * Sets the confidence thresholds for accepting/rejecting a detected language
   * @param configResourceName JSON file with the thresholds  
   * @throws LangDetectionServiceConfigurationException
   */
  public void loadThresholds(String configResourceName)
      throws LangDetectionServiceConfigurationException {
    if(!StringUtils.isEmpty(configResourceName))
        thresholdsConf = ThresholdsConfiguration.fromJson(configResourceName);
  }
}
