package eu.europeana.api.translation.service;

import java.util.List;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

/**
 * Base class for language detection services with empty implementation of optional methods
 */
public abstract class AbstractLanguageDetectionService implements LanguageDetectionService {

  private ThresholdsConfiguration thresholdsConf;
  private List<LanguageDetectionService> referencedServices;
  private String serviceId;
  
  @Override
  public void setThresholdsConf(ThresholdsConfiguration thresholdsConf) {
    this.thresholdsConf = thresholdsConf;
  }

  @Override
  public String getExternalServiceEndPoint() {
    // overwrite in subclasses when needed
    return "";
  }
  
  @Override
  public String getServiceId() {
    return serviceId;
  }

  @Override
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
  
  public ThresholdsConfiguration getThresholdsConf() {
    return thresholdsConf;
  }
  
  @Override
  public List<LanguageDetectionService> getReferencedServices() {
    return referencedServices;
  }

  @Override
  public void setReferencedServices(List<LanguageDetectionService> services) {
    this.referencedServices = services;
  }
}
