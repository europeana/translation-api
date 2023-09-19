package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaApiException;

/** 
 * Exception thrown when the Google quota limit for translate/detect has been reached  
*/
public class GoogleResourceExhaustedException extends EuropeanaApiException {

  private static final long serialVersionUID = 6996489981469137484L;

  public GoogleResourceExhaustedException(String msg) {
    super(msg);
  }

  @Override
  public boolean doLog() {
    return false;
  }

  @Override
  public boolean doLogStacktrace() {
    return false;
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.GATEWAY_TIMEOUT;
  }
}
