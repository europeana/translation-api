package eu.europeana.api.translation.service.hybrid;

import java.util.List;
import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.AbstractLanguageDetectionService;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

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
public class HybridLangDetectService extends AbstractLanguageDetectionService implements LanguageDetectionService {

  private List<LanguageDetectionService> referencedServices;
  private String serviceId;

  /**
   * Constructor using referenced services as array
   * 
   * @param referencedServices language detection services to be used by the hybrid
   *                 detector, ordered by priority in descending order
   */
  public HybridLangDetectService() {
    //default constructor without services
  }
  
  /**
   * Constructor using referenced services as array
   * 
   * @param services language detection services to be used by the hybrid
   *                 detector, ordered by priority in descending order
   */
  public HybridLangDetectService(LanguageDetectionService... services) {
    this.referencedServices = List.of(services);
  }

  /**
   * Constructor providing referenced services as list
   * 
   * @param services language detection services to be used by the hybrid
   *                 detector, ordered by priority in descending order
   */
  public HybridLangDetectService(List<LanguageDetectionService> services) {
    this.referencedServices = services;
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
      if (obj.getDetectedLang() != null)
        break;
    }
  }

  @Override
  public void close() {
    //nothing to do
  }

  @Override
  public String getServiceId() {
    return serviceId;
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

  @Override
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  @Override
  public boolean isSupported(String srcLang) {
    for (LanguageDetectionService service : referencedServices) {
      if (service.isSupported(srcLang))
        return true;
    }
    return false;
  }

  @Override
  public void setThresholdsConf(ThresholdsConfiguration thresholdsConf) {
    //not used for hybrid implementation
    
  }

  @Override
  public List<LanguageDetectionService> getReferencedServices() {
    return referencedServices;
  }

  public void setReferencedServices(List<LanguageDetectionService> services) {
    this.referencedServices = services;
  }

}
