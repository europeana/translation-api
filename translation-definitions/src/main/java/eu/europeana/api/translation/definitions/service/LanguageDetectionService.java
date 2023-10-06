package eu.europeana.api.translation.definitions.service;

import java.util.List;
import eu.europeana.api.translation.definitions.service.exception.LanguageDetectionException;

public interface LanguageDetectionService {

    boolean isSupported(String srcLang);
    
    /**
     * The id used to register the service
     * @return the serviceId
     */
    String getServiceId();
    
    void setServiceId(String serviceId);

    /**
     * To fetch the source language for the list of texts.
     * If passed, langHint is used a hint in the method
     * @param texts to detect source language
     * @param langHint optional, hint to identify source language in which the texts are available
     * @return
     * @throws LanguageDetectionException
     */

    List<String> detectLang(List<String> texts, String langHint) throws LanguageDetectionException;

    /**
     * to close the engine
     */
    void close();
    
    /**
     * The external endpoint invoked by the service
     * @return
     */
    public String getExternalServiceEndPoint();

}