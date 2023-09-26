package eu.europeana.api.translation.web.exception;

import org.springframework.http.HttpStatus;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;

public class ParamValidationException extends EuropeanaI18nApiException {

  private static final long serialVersionUID = -2892184722732646887L;

  public ParamValidationException(String msg, String errorCode, String i18nKey, String[] i18nParams) {
    this(msg, errorCode, i18nKey, i18nParams, null);
  }

  public ParamValidationException(String msg, String errorCode, String i18nKey, String[] i18nParams, Throwable th) {
    super(msg, errorCode, i18nKey, i18nParams);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.BAD_REQUEST;
  }
}
