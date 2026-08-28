package eu.europeana.api.translation.service;

import java.util.List;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

/**
 * Base class for language detection services with empty implementation of optional methods
 */
public abstract class AbstractLanguageDetectionService implements LanguageDetectionService {

  private ThresholdsConfiguration thresholdsConf;
  private List<LanguageDetectionService> referencedServices;
  private String serviceId;
  private RelatedLanguages relatedLanguages;
  
  @Override
  public void setThresholdsConf(ThresholdsConfiguration thresholdsConf) throws LangDetectionServiceConfigurationException {
    this.thresholdsConf = thresholdsConf;
  }

  @Override
  public String getExternalServiceEndPoint() {
    // overwrite in subclasses when needed
    return "#no-endpoint"; //this method does not need to be abstract, it might not be needed after removing pangeanic support completely
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
    this.referencedServices = List.copyOf(services);
  }

  public RelatedLanguages getRelatedLanguages() {
    return relatedLanguages;
  }

  public void setRelatedLanguages(RelatedLanguages relatedLanguages) {
    this.relatedLanguages = relatedLanguages;
  }
}
