package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaApiException;

/**
 * Exception that is thrown when there is an error using the translation service
 */
public class TranslationException extends EuropeanaApiException {

    /**
   * 
   */
  private static final long serialVersionUID = -435977863896735850L;

    public TranslationException(String msg) {
        super(msg);
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}