package eu.europeana.api.translation.web.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import eu.europeana.api.commons_sb3.error.EuropeanaI18nApiException;

/**
 * Exception thrown when something wrong happens in the call to the external translate/detect
 * services.
 */
public class ExternalServiceCallException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = -6713841065610985800L;

  public ExternalServiceCallException(String msg, String errorCode, String error,
      HttpStatus responseStatus, String i18nKey, List<String> i18nParams, Throwable th) {
    super(msg, errorCode, error, responseStatus, i18nKey, i18nParams, th);

  }

}
