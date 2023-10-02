package eu.europeana.api.translation.web.exception;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import eu.europeana.api.commons.config.i18n.I18nService;
import eu.europeana.api.commons.error.EuropeanaApiErrorResponse;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;
import eu.europeana.api.commons.web.exception.EuropeanaGlobalExceptionHandler;
import eu.europeana.api.commons.web.exception.HttpException;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.web.service.RequestPathMethodService;

@ControllerAdvice
@ConditionalOnWebApplication
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {

  I18nService i18nService;

  protected I18nService getI18nService() {
    return i18nService;
  }

  @Autowired
  public GlobalExceptionHandler(RequestPathMethodService requestPathMethodService,
      @Qualifier(BeanNames.BEAN_I18N_SERVICE) I18nService i18nService) {
    this.requestPathMethodService = requestPathMethodService;
    this.i18nService = i18nService;
  }

  
  /**
   * Default handler for EuropeanaI18nApiException types
   *
   * @param e caught exception
   */
  @ExceptionHandler(EuropeanaI18nApiException.class)
  public ResponseEntity<EuropeanaApiErrorResponse> handleEuropeanaApiException(
      EuropeanaI18nApiException e, HttpServletRequest httpRequest) {

    return buildApiErrorResponse(e, httpRequest, e.getI18nKey(), e.getI18nParams());
  }

  private ResponseEntity<EuropeanaApiErrorResponse> buildApiErrorResponse(EuropeanaApiException e,
      HttpServletRequest httpRequest, String i18nKey, String[] i18NParams) {
    EuropeanaApiErrorResponse response =
        new EuropeanaApiErrorResponse.Builder(httpRequest, e, stackTraceEnabled())
            .setStatus(e.getResponseStatus().value())
            .setError(e.getResponseStatus().getReasonPhrase())
            .setMessage(e.doExposeMessage() ? buildResponseMessage(e, i18nKey, i18NParams) : null)
            // code only included in JSON if a value is set in exception
            .setCode(e.getErrorCode()).build();
    return ResponseEntity.status(e.getResponseStatus()).headers(createHttpHeaders(httpRequest))
        .body(response);
  }
  
  private String buildResponseMessage(Exception e, String i18nKey, String[] i18nParams) {
    if (StringUtils.isBlank(i18nKey)) {
      return e.getMessage();
    } else {
      return i18nService.getMessage(i18nKey, i18nParams);
    }
  }


  /**
   * Default handler for EuropeanaI18nApiException types
   *
   * @param e caught exception
   */
  @ExceptionHandler(HttpException.class)
  public ResponseEntity<EuropeanaApiErrorResponse> handleCommonHttpException(
      HttpException e, HttpServletRequest httpRequest) {
    EuropeanaApiErrorResponse response =
        new EuropeanaApiErrorResponse.Builder(httpRequest, e, stackTraceEnabled())
            .setStatus(e.getStatus().value())
            .setError(e.getStatus().getReasonPhrase())
            .setMessage( i18nService.getMessage(e.getI18nKey(), e.getI18nParams()))
            // code only included in JSON if a value is set in exception
            .setCode(e.getI18nKey()).build();
    return ResponseEntity.status(e.getStatus()).headers(createHttpHeaders(httpRequest))
        .body(response);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<EuropeanaApiErrorResponse> handleNoHandlerFoundException(
      NoHandlerFoundException e, HttpServletRequest httpRequest) {
    EuropeanaApiErrorResponse response =
        new EuropeanaApiErrorResponse.Builder(httpRequest, e, stackTraceEnabled())
            .setStatus(HttpStatus.NOT_FOUND.value())
            .setError(HttpStatus.NOT_FOUND.getReasonPhrase()).setMessage(e.getMessage()).build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND.value())
        .contentType(MediaType.APPLICATION_JSON).body(response);
  }

}
