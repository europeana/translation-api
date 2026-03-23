package eu.europeana.api.translation.web.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import eu.europeana.api.commons_sb3.error.EuropeanaI18nApiException;

/**
 * Exception thrown when the Google quota limit for translate/detect has been reached
 */
public class GoogleResourceExhaustedException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = 3093354601849276359L;

  /**
   * Exception indicated that the google service quota was exceeded
   * @param msg error message
   * @param errorCode error code id available
   * @param error label indicating the type of error. 
   * @param i18nKey the key for retrieving the i18n message for API Response serialization
   * @param i18nParams the params for generating the i18n message during API Response serialization
   * @param th original exception
   */
  public GoogleResourceExhaustedException(String msg, String errorCode, String error, HttpStatus httpStatus, String i18nKey,
      List<String> i18nParams, Throwable th) {
    super(msg, errorCode, error, httpStatus, i18nKey, i18nParams, th);
  }

}
