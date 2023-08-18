package eu.europeana.api.translation.service;

import java.util.List;
import eu.europeana.api.translation.service.exception.TranslationException;

public interface LanguageDetectionService {

    boolean isSupported(String srcLang);

    /**
     * To fetch the source language for the list of texts.
     * If passed, langHint is used a hint in the method
     * @param texts to detect source language
     * @param langHint optional, hint to identify source language in which the texts are available
     * @return
     * @throws TranslationException
     */

    List<String> detectLang(List<String> texts, String langHint) throws TranslationException;

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
