package eu.europeana.api.translation.service.hybrid;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.AbstractLanguageDetectionService;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * Language detection service that applies multiple language detectors. The
 * behaviour of the hybrid detector is as follow: - Is is configured with a list
 * of detectors (any implementations of LanguageDetectionService may be used) -
 * For detecting the language of a text, the hybrid detector starts by sending
 * the request to the first detector on the list. If it detects a language, then
 * the detected language is returned; if not then the request is sent to the
 * second detector. This procedure continues until the one of the detectors
 * returns a result. It none of the detectors returns the result, the hint or
 * null is returned.
 * 
 * @author Nuno Freire
 * @since 05/02/2025
 */
public class HybridLangDetectService extends AbstractLanguageDetectionService{

  private final Logger logger = LogManager.getLogger(getClass());
  
  /**
   * Constructor using referenced services as array
   * 
   * @param referencedServices language detection services to be used by the hybrid
   *                 detector, ordered by priority in descending order
   */
  public HybridLangDetectService() {
    //default constructor without services
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
    if (languageDetectionObjs.isEmpty()) {
      return;
    }

    for (LanguageDetectionObj obj : languageDetectionObjs) {
      //create temporary hint from request and reset hint
      String savedHint = obj.getHint();
      obj.setHint(null);
      delegateLanguageDetection(obj);
      //use hint if not detected with good confidence
      if (obj.getDetectedLang() == null) {
        obj.setDetectedLang(savedHint);
      }
      //restore hint
      obj.setHint(savedHint);
    }
  }

  private void delegateLanguageDetection(LanguageDetectionObj obj)
      throws LanguageDetectionException {
    
    if(getReferencedServices() == null || getReferencedServices().isEmpty()) {
      return;
    }
      
    List<LanguageDetectionObj> isolatedObj = List.of(obj);
    
    for (LanguageDetectionService service : getReferencedServices()) {
      service.detectLang(isolatedObj);
      if (obj.getDetectedLang() != null) {
        if(logger.isDebugEnabled()) {
          logger.debug("Language: {} detected with service: {} for text: {} ", obj.getDetectedLang(), service.getServiceId(), obj.getText());
        }
        break;
      } else {
        if(logger.isDebugEnabled()) {
          logger.debug("No Language: detected with service: {} with hint: {} for text: {} ", service.getServiceId(), obj.getHint(), obj.getText());
        }
      }
    }
  }

  @Override
  public void close() {
    //nothing to do
  }

  @Override
  public boolean isSupported(String srcLang) {
    for (LanguageDetectionService service : getReferencedServices()) {
      if (service.isSupported(srcLang))
        return true;
    }
    return false;
  }

}
