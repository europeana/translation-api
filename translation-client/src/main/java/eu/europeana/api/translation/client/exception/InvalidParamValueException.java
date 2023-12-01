package eu.europeana.api.translation.client.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

public class InvalidParamValueException extends EuropeanaApiException {

    public InvalidParamValueException(String msg) {
        super(msg);
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}