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

  @Deprecated
  /**
   * Use the method translate(List<TranslationObj> translationObjs).
   * Translate multiple texts
   * 
   * @param texts to translate
   * @param targetLanguage language into which the texts are translated
   * @return translations of the provided texts
   * @throws TranslationException when there is a problem sending the translation request
   */
  List<String> translate(List<String> texts, String targetLanguage) throws TranslationException;

  /**
   * Translates a list of texts from a list of TranslationObj and saves the results back to the objects.
   * @param translationObjs
   * @throws TranslationException
   */
  void translate(List<TranslationObj> translationObjs, boolean detectLanguages) throws TranslationException;
  
  void detectLanguages(List<TranslationObj> translationObjs) throws TranslationException;
  
  @Deprecated
  /**
   * Use the method translate(List<TranslationObj> translationObjs).
   * Translate multiple texts.
   * 
   * @param texts to translate
   * @param targetLanguage language into which the texts are translated
   * @param sourceLanguage source language of the texts to be translated
   * @return translations of the provided texts
   * @throws TranslationException when there is a problem sending the translation request
   */
  List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage)
      throws TranslationException;


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

