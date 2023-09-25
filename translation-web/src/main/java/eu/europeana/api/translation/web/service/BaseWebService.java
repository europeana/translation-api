package eu.europeana.api.translation.web.service;

import com.google.api.gax.rpc.ResourceExhaustedException;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.translation.definitions.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.definitions.service.exception.TranslationException;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.web.exception.ExternalServiceCallException;
import eu.europeana.api.translation.web.exception.GoogleResourceExhaustedException;

public class BaseWebService {
  
  protected void throwApiException(LanguageDetectionException ex) throws EuropeanaApiException {
    if(ex.getCause() instanceof ResourceExhaustedException) {
      throw new GoogleResourceExhaustedException(null, null, TranslationAppConstants.ERROR_GOOGLE_QUOTA_LIMIT, null);
    }
    throw new ExternalServiceCallException(null, null, TranslationAppConstants.ERROR_LANG_DETECT_SERVICE_CALL, null);
  }

  protected void throwApiException(TranslationException ex) throws EuropeanaApiException {
    if(ex.getCause() instanceof ResourceExhaustedException) {
      throw new GoogleResourceExhaustedException(null, null, TranslationAppConstants.ERROR_GOOGLE_QUOTA_LIMIT, null);
    }
    throw new ExternalServiceCallException(null, null, TranslationAppConstants.ERROR_TRANSLATION_SERVICE_CALL, null);
  }

}
