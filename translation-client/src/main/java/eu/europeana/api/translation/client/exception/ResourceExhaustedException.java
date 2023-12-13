package eu.europeana.api.translation.client.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

public class ResourceExhaustedException extends EuropeanaApiException {

    public ResourceExhaustedException(String msg) {
        super(msg);
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}