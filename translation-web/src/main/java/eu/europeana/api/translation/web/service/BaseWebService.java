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
      throw new GoogleResourceExhaustedException(TranslationAppConstants.GOOGLE_QUOTA_LIMIT_MSG);
    }
    throw new ExternalServiceCallException(TranslationAppConstants.LANG_DETECT_SERVICE_EXCEPTION_MSG);
  }

  protected void throwApiException(TranslationException ex) throws EuropeanaApiException {
    if(ex.getCause() instanceof ResourceExhaustedException) {
      throw new GoogleResourceExhaustedException(TranslationAppConstants.GOOGLE_QUOTA_LIMIT_MSG);
    }
    throw new ExternalServiceCallException(TranslationAppConstants.TRANSLATION_SERVICE_EXCEPTION_MSG);
  }

}
