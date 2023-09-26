package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;

/** 
 * Exception thrown when something wrong happens in the call to the external translate/detect services.
*/
public class ExternalServiceCallException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = -6713841065610985800L;

  public ExternalServiceCallException(String msg, String errorCode, HttpStatus responseStatus, String i18nKey, String[] i18nParams, Throwable th) {
    super(msg, errorCode, responseStatus, i18nKey, i18nParams, th);
   
  }

}
