package eu.europeana.api.translation.service.hybrid;

import java.util.Arrays;
import java.util.List;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
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
public class HybridLangDetectService implements LanguageDetectionService {

  private final LanguageDetectionService[] services;
  private String serviceId;

  /**
   * Default constructor
   * 
   * @param services language detection services to be used by the hybrid
   *                 detector, ordered by priority in descending order
   */
  public HybridLangDetectService(LanguageDetectionService... services) {
    this.services = Arrays.copyOf(services, services.length);
  }

  @Override
  public void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException {
    if (languageDetectionObjs.isEmpty()) {
      return;
    }

    for (LanguageDetectionObj obj : languageDetectionObjs) {
      String savedHint = obj.getHint();
      obj.setHint(null);
      List<LanguageDetectionObj> isolatedObj = List.of(obj);
      for (LanguageDetectionService service : services) {
        service.detectLang(isolatedObj);
        if (obj.getDetectedLang() != null)
          break;
      }
      if (obj.getDetectedLang() == null)
        obj.setDetectedLang(savedHint);
      obj.setHint(savedHint);
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
    for (LanguageDetectionService service : services) {
      if (service.isSupported(srcLang))
        return true;
    }
    return false;
  }

}
