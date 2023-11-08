package eu.europeana.api.translation.service;

import java.util.List;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

/**
 * The interface for the language detection services
 * 
 * @author GordeaS
 *
 */
public interface LanguageDetectionService {

  /**
   * indicates if the given language is supported by the service implementing the interface
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
   * To fetch the source language for the list of texts. If passed, langHint is used a hint in the
   * method
   * 
   * @param texts to detect source language
   * @param langHint optional, hint to identify source language in which the texts are available
   * @return the list containing detected language for each line of text 
   * @throws LanguageDetectionException if an error occurred when invoking the external service
   */
  List<String> detectLang(List<String> texts, String langHint) throws LanguageDetectionException;

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
