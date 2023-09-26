package eu.europeana.api.translation.web.exception;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;

/** 
 * Exception thrown when the Google quota limit for translate/detect has been reached  
*/
public class GoogleResourceExhaustedException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = 3093354601849276359L;

  public GoogleResourceExhaustedException(String msg, String errorCode, String i18nKey, String[] i18nParams, Throwable th) {
    super(msg, errorCode, GATEWAY_TIMEOUT, i18nKey, i18nParams, th);
  }
  
}
