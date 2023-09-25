package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;

/** 
 * Exception thrown when the Google quota limit for translate/detect has been reached  
*/
public class GoogleResourceExhaustedException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = 3093354601849276359L;

  public GoogleResourceExhaustedException(String msg, String errorCode, String i18nKey, String[] i18nParams) {
    super(msg, errorCode, i18nKey, i18nParams);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.GATEWAY_TIMEOUT;
  }
}
