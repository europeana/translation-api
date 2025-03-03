package eu.europeana.api.translation.service.hybrid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
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

  protected static final Logger LOG = LogManager.getLogger(HybridLangDetectService.class);
  private final List<LanguageDetectionService> services;
  private String serviceId;

  public HybridLangDetectService() {
    services = new ArrayList<>();
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

  @Override
  public boolean isSupported(String srcLang) {
    for (LanguageDetectionService service : services) {
      if (service.isSupported(srcLang))
        return true;
    }
    return false;
  }

  @Override
  public void setConfiguration(Map<String, LanguageDetectionService> detectionServices, String configResourceName)
      throws LangDetectionServiceConfigurationException {
    HybridServiceConfiguration config = null;
    try (InputStream inputStream = getClass().getResourceAsStream(configResourceName)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      config = parseConfig(reader);
      LOG.info("Successfully loaded service configurations from classpath resources.");
    } catch (IOException e) {
      throw new LangDetectionServiceConfigurationException(
          "Cannot read service configurations from classpath resource!", e);
    }
    for (String serviceId : config.getServices()) {
      LanguageDetectionService subService = detectionServices.get(serviceId);
      if (subService == null)
        throw new LangDetectionServiceConfigurationException("Service ID not found: " + serviceId);
      services.add(subService);
    }
  }

  private HybridServiceConfiguration parseConfig(BufferedReader reader) throws JsonProcessingException {
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    return new ObjectMapper().readValue(content, HybridServiceConfiguration.class);
  }

}
