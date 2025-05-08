package eu.europeana.api.translation.service;

import java.util.List;
import eu.europeana.api.translation.service.threshold.ThresholdsConfiguration;

public abstract class AbstractLanguageDetectionService implements LanguageDetectionService {

  public List<LanguageDetectionService> getReferencedServices() {
    return null;
  }

  public void setReferencedServices(List<LanguageDetectionService> services) {
    // overwrite in subclasses when needed
  }

  @Override
  public void setThresholdsConf(ThresholdsConfiguration thresholdsConf) {
    // overwrite in subclasses when needed
  }

  @Override
  public String getExternalServiceEndPoint() {
    // overwrite in subclasses when needed
    return null;
  }
}
