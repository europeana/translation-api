package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaApiException;

public class ParamValidationException extends EuropeanaApiException {

  private static final long serialVersionUID = -6256917812002322197L;

  public ParamValidationException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.BAD_REQUEST;
  }
}
