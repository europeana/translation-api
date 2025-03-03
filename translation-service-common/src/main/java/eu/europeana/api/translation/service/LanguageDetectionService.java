package eu.europeana.api.translation.service;

import java.util.List;
import java.util.Map;

import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.service.exception.LangDetectionServiceConfigurationException;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.exception.TranslationServiceConfigurationException;

/**
 * The interface for the language detection services
 * 
 * @author GordeaS
 *
 */
public interface LanguageDetectionService {

  /**
   * indicates if the given language is supported by the service implementing the
   * interface
   * 
   * @param srcLang language hint
   * @return true is supported
   */
  boolean isSupported(String srcLang);

  /**
   * The id used to register the service
   * 
   * @return the serviceId
   */
  String getServiceId();

  void setServiceId(String serviceId);

  /**
   * Configures the service
   * 
   * @param detectionSetvices all the available LanguageDetectionServices (needed
   *                          for hybrid language detectors)
   * @param resourceName      a resource available in the classpath with the
   *                          configuration of the service
   * @throws TranslationServiceConfigurationException 
   */
  void setConfiguration(Map<String, LanguageDetectionService> detectionServices, String configResourceName) throws LangDetectionServiceConfigurationException;

  /**
   * To fetch the source language for the list of texts. If passed, langHint is
   * used a hint in the method
   * 
   * @param languageDetectionObjs languge detection input object list
   * @throws LanguageDetectionException if an error occurred when invoking the
   *                                    external service
   */
  void detectLang(List<LanguageDetectionObj> languageDetectionObjs) throws LanguageDetectionException;

  /**
   * to close the engine
   */
  void close();

  /**
   * The external endpoint invoked by the service
   * 
   * @return the endpoint of the external service
   */
  public String getExternalServiceEndPoint();

}
