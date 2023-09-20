package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaApiException;

/** 
 * Exception thrown when something wrong happens in the call to the external translate/detect services
*/
public class ExternalServiceCallException extends EuropeanaApiException {

  private static final long serialVersionUID = 3094915543154136874L;

  public ExternalServiceCallException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
