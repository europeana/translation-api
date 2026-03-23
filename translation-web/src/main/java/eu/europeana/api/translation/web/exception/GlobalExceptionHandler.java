package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import eu.europeana.api.commons_sb3.error.EuropeanaApiErrorResponse;
import eu.europeana.api.commons_sb3.error.EuropeanaGlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {


  /**
   * HttpMessageNotReadableException thrown when the request body is not parsable to the declared input of the handler method
   * @param e the exception indicating the request message parsing error
   * @param httpRequest the request object
   * @return the api response
   */
  @ExceptionHandler
  public ResponseEntity<EuropeanaApiErrorResponse> handleRequestBodyNotParsableError(HttpMessageNotReadableException e, HttpServletRequest httpRequest) {
      HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
      EuropeanaApiErrorResponse response = (new EuropeanaApiErrorResponse.Builder(httpRequest, e, stackTraceEnabled()))
              .setStatus(responseStatus.value())
              .setError(responseStatus.getReasonPhrase())
              .setMessage("Invalid request body: " + e.getMessage())
              .build();

      return ResponseEntity
              .status(responseStatus)
              .headers(createHttpHeaders(httpRequest))
              .body(response);
  }
  
}
