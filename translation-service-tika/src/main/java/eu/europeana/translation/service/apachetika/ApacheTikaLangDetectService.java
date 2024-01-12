package eu.europeana.translation.service.apachetika;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

public class ApacheTikaLangDetectService implements LanguageDetectionService {

  protected static final Logger LOG = LogManager.getLogger(ApacheTikaLangDetectService.class);
  private LanguageDetector detector; 
  private String serviceId;

  private Set<String> supportedLanguages = Set.of("af", "an", "ar", "ast", "be", "br", "ca", "bg",
      "bn", "cs", "cy", "da", "de", "el", "en", "es", "et", "eu", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi",
      "hr", "ht", "hu", "id", "is", "it", "ja", "km", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ms", "mt",
      "ne", "nl", "no", "oc", "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl",
      "tr", "uk", "ur", "vi", "wa", "yi", "zh-cn", "zh-tw");

  public ApacheTikaLangDetectService() {
    this.detector = new OptimaizeLangDetector().loadModels();
  }

  @Override
  public boolean isSupported(String srcLang) {
    return supportedLanguages.contains(srcLang);
  }

  @Override
  public List<String> detectLang(List<String> texts, String langHint) throws LanguageDetectionException {
    if (texts.isEmpty()) {
      return Collections.emptyList();
    }
    
    List<String> detectedLangs = new ArrayList<>(texts.size());
    List<LanguageResult> tikaLanguages=null;
    for(String text : texts) {
      //returns all tika languages sorted by score
      tikaLanguages =  this.detector.detectAll(text);

      detectedLangs.add(chooseDetectedLang(tikaLanguages, langHint));
      
    }
    return detectedLangs;
  }

  /**
   * In case lang hint is not null, check if it myabe exists among the langs with the highest confidence, 
   * and if so return the langHint as a detected lang, if not return the first one. 
   */
  private String chooseDetectedLang(List<LanguageResult> tikaLanguages, String langHint) {
    if(tikaLanguages.isEmpty()) {
      return null;
    }
    //if langHint is null, return the first detected language (has the highest confidence)
    if(StringUtils.isBlank(langHint)) {
      return tikaLanguages.get(0).getLanguage();
    }

    String detectedLang=tikaLanguages.get(0).getLanguage();
    if(langHint.equals(detectedLang)) {
      return langHint;
    }
    float confidence=tikaLanguages.get(0).getRawScore();
    for(int i=1;i<tikaLanguages.size();i++) {
      if(tikaLanguages.get(i).getRawScore()>=confidence) {
        if(langHint.equals(tikaLanguages.get(i).getLanguage())) {
          detectedLang=langHint;
          break;
        }
      } else {
        break;
      }        
    }
    return detectedLang;
  }

  @Override
  public void close() {
  }

  @Override
  public String getServiceId() {
    return serviceId;
  }

  @Override
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

}