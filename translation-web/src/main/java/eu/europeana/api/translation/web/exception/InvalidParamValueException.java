package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaApiException;

public class InvalidParamValueException extends EuropeanaApiException {

    /**
   * 
   */
  private static final long serialVersionUID = 5354834725828906573L;

    public InvalidParamValueException(String msg) {
        super(msg);
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}