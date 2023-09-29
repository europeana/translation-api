package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;

public class ParamValidationException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = -2892184722732646887L;

  /**
   * Constructor for exception indicating invalid request parameters
   * @param msg error message
   * @param errorCode optional error code
   * @param i18nKey the key for retrieving the i18n message for API Response serialization
   * @param i18nParams the params for generating the i18n message during API Response serialization
   */
  public ParamValidationException(String msg, String errorCode, String i18nKey, String[] i18nParams) {
    this(msg, errorCode, i18nKey, i18nParams, null);
  }

  /**
   * Constructor for exception indicating invalid request parameters
   * @param msg error message
   * @param errorCode optional error code
   * @param i18nKey the key for retrieving the i18n message for API Response serialization
   * @param i18nParams the params for generating the i18n message during API Response serialization
   * @param th original exception
   */
  public ParamValidationException(String msg, String errorCode, String i18nKey, String[] i18nParams, Throwable th) {
    super(msg, errorCode, HttpStatus.BAD_REQUEST, i18nKey, i18nParams, th);
  }
    
}
