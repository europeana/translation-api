package eu.europeana.api.translation.web.exception;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import eu.europeana.api.commons.config.i18n.I18nService;
import eu.europeana.api.commons.error.EuropeanaApiErrorResponse;
import eu.europeana.api.commons.web.exception.EuropeanaGlobalExceptionHandler;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.web.service.RequestPathMethodService;

@ControllerAdvice
@ConditionalOnWebApplication
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {

  I18nService i18nService;

  /**
   * Constructor for the initialization of the Exception handler
   * @param requestPathMethodService builtin service for path method mapping
   * @param i18nService the internationalization service
   */
  @Autowired
  public GlobalExceptionHandler(RequestPathMethodService requestPathMethodService,
      @Qualifier(BeanNames.BEAN_I18N_SERVICE) I18nService i18nService) {
    this.requestPathMethodService = requestPathMethodService;
    this.i18nService = i18nService;
  }

  @Override
  public I18nService getI18nService() {
    return i18nService;
  }

  
  /**
   * HttpMessageNotReadableException thrown when a required parameter is not included in a request.
   * @param e the exception indicating the request message parsing error
   * @param httpRequest the request object
   */
  @ExceptionHandler
  public ResponseEntity<EuropeanaApiErrorResponse> handleInputValidationError(HttpMessageNotReadableException e, HttpServletRequest httpRequest) {
      HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
      EuropeanaApiErrorResponse response = (new EuropeanaApiErrorResponse.Builder(httpRequest, e, stackTraceEnabled()))
              .setStatus(responseStatus.value())
              .setError(responseStatus.getReasonPhrase())
              .setMessage("Invalid request body: " + e.getMessage())
              .setSeeAlso(getSeeAlso())
              .build();

      return ResponseEntity
              .status(responseStatus)
              .headers(createHttpHeaders(httpRequest))
              .body(response);
  }
}
