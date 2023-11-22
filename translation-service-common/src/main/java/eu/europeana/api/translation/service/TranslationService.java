package eu.europeana.api.translation.service;

import java.util.List;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.exception.TranslationException;

/**
 * Generic translation service interface
 */
public interface TranslationService {


  /**
   * The id used to register the service
   * @return the serviceId
   */
  String getServiceId();
  
  void setServiceId(String serviceId);

  /**
   * To validate the given pair of source and target language is valid for translation
   * 
   * @param srcLang source language of the data to be translated
   * @param trgLang target language in which data has to be translated
   * @return true is the pair is valid
   */
  boolean isSupported(String srcLang, String trgLang);

  /**
   * Translates a list of texts from a list of TranslationObj and saves the results back to the objects.
   * @param translationObjs
   * @throws TranslationException
   */
  void translate(List<TranslationObj> translationObjs) throws TranslationException;
    
  /**
   * to close the engine
   */
  void close();

  /**
   * The external endpoint invoked by the service
   * 
   * @return the external endpoint
   */
  String getExternalServiceEndPoint();


}

