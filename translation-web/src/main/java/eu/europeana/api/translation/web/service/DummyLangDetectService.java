package eu.europeana.api.translation.web.service;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.europeana.api.translation.definitions.service.LanguageDetectionService;
import eu.europeana.api.translation.definitions.service.exception.LanguageDetectionException;

public class DummyLangDetectService implements LanguageDetectionService {

  protected static final Logger LOG = LogManager.getLogger(DummyLangDetectService.class);
  private final String serviceId = "DUMMY";

  public DummyLangDetectService() {
  }

  @Override
  public boolean isSupported(String srcLang) {
    return true;
  }

  @Override
  public List<String> detectLang(List<String> texts, String langHint)
      throws LanguageDetectionException {
    return texts;
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
  }

  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

}
