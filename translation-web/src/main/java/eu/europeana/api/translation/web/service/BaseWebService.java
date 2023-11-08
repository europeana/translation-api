package eu.europeana.api.translation.web.service;

import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_GOOGLE_QUOTA_LIMIT;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_LANG_DETECT_SERVICE_CALL;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_TRANSLATION_SERVICE_CALL;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import com.google.api.gax.rpc.ResourceExhaustedException;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.web.exception.ExternalServiceCallException;
import eu.europeana.api.translation.web.exception.GoogleResourceExhaustedException;

public class BaseWebService {

  protected void throwApiException(LanguageDetectionException ex) throws EuropeanaI18nApiException {
    if (ex.getCause() instanceof ResourceExhaustedException) {
      throw new GoogleResourceExhaustedException(ex.getMessage(), ERROR_GOOGLE_QUOTA_LIMIT,
          ERROR_GOOGLE_QUOTA_LIMIT, null, ex);
    }
    throw new ExternalServiceCallException(ex.getMessage(), ERROR_LANG_DETECT_SERVICE_CALL,
        //the remote status code could be used here  ex.getRemoteStatusCode()),
        GATEWAY_TIMEOUT,
        ERROR_LANG_DETECT_SERVICE_CALL, null, ex);
  }

  protected void throwApiException(TranslationException ex) throws EuropeanaI18nApiException {
    if (ex.getCause() instanceof ResourceExhaustedException) {
      throw new GoogleResourceExhaustedException(ERROR_GOOGLE_QUOTA_LIMIT,
          null, ERROR_GOOGLE_QUOTA_LIMIT, null, ex);
    }
    throw new ExternalServiceCallException(ex.getMessage(), ERROR_TRANSLATION_SERVICE_CALL,
        //the remote status code could be used here  ex.getRemoteStatusCode()),
        GATEWAY_TIMEOUT,
        ERROR_TRANSLATION_SERVICE_CALL, null, ex);
  }

}
