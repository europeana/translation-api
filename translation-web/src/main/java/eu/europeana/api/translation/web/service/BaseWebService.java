package eu.europeana.api.translation.web.service;

import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.ERROR_GOOGLE_QUOTA_LIMIT;
import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.ERROR_LANG_DETECT_SERVICE_CALL;
import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.ERROR_TRANSLATION_SERVICE_CALL;
import org.springframework.http.HttpStatus;
import com.google.api.gax.rpc.ResourceExhaustedException;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.translation.definitions.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.definitions.service.exception.TranslationException;
import eu.europeana.api.translation.web.exception.ExternalServiceCallException;
import eu.europeana.api.translation.web.exception.GoogleResourceExhaustedException;

public class BaseWebService {

  protected void throwApiException(LanguageDetectionException ex) throws EuropeanaApiException {
    if (ex.getCause() instanceof ResourceExhaustedException) {
      throw new GoogleResourceExhaustedException(ex.getMessage(), ERROR_GOOGLE_QUOTA_LIMIT,
          ERROR_GOOGLE_QUOTA_LIMIT, null, ex);
    }
    throw new ExternalServiceCallException(ex.getMessage(), ERROR_LANG_DETECT_SERVICE_CALL,
        HttpStatus.resolve(ex.getRemoteStatusCode()),
        ERROR_LANG_DETECT_SERVICE_CALL, null, ex);
  }

  protected void throwApiException(TranslationException ex) throws EuropeanaApiException {
    if (ex.getCause() instanceof ResourceExhaustedException) {
      throw new GoogleResourceExhaustedException(ERROR_GOOGLE_QUOTA_LIMIT,
          null, ERROR_GOOGLE_QUOTA_LIMIT, null, ex);
    }
    throw new ExternalServiceCallException(ex.getMessage(), ERROR_TRANSLATION_SERVICE_CALL,
        HttpStatus.resolve(ex.getRemoteStatusCode()),
        ERROR_TRANSLATION_SERVICE_CALL, null, ex);
  }

}
