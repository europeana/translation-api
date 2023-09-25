package eu.europeana.api.translation.web.exception;

import eu.europeana.api.commons.error.EuropeanaI18nApiException;

/** 
 * Exception thrown when something wrong happens in the call to the external translate/detect services.
*/
public class ExternalServiceCallException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = -6713841065610985800L;

  public ExternalServiceCallException(String msg, String errorCode, String i18nKey, String[] i18nParams) {
    super(msg, errorCode, i18nKey, i18nParams);
  }

}
