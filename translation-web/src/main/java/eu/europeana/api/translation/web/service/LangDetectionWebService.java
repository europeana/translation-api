package eu.europeana.api.translation.web.service;

import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_INVALID_PARAM_VALUE;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_UNSUPPORTED_LANG;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

import eu.europeana.api.translation.definitions.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;
import eu.europeana.api.translation.config.TranslationServiceProvider;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;
import eu.europeana.api.translation.web.exception.ParamValidationException;

@Service
public class LangDetectionWebService extends BaseWebService {

  @Autowired
  private TranslationServiceProvider translationServiceProvider;

  private final Logger logger = LogManager.getLogger(getClass());

  public LangDetectResponse detectLang(LangDetectRequest langDetectRequest)
      throws EuropeanaI18nApiException {
    List<LanguageDetectionObj> languageDetectionObjs = buildLangDetectionObjectList(langDetectRequest);

    LanguageDetectionService langDetectService = getLangDetectService(langDetectRequest);
    LanguageDetectionService fallback = getFallbackService(langDetectRequest);
    String serviceId = null;
    List<LanguageDetectionObj> filteredObjs = null;
    try {
      // preprocess the values
      translationServiceProvider.getLanguageDetectionPreProcessor().detectLang(languageDetectionObjs);
      // send the values which are not yet translated (isTranslated=false)
      filteredObjs = languageDetectionObjs.stream().filter(to -> !to.isTranslated()).collect(Collectors.toList());
      langDetectService.detectLang(filteredObjs);
      serviceId = langDetectService.getServiceId();
    } catch (LanguageDetectionException originalError) {
      // check if fallback is available
      if (fallback == null) {
        throwApiException(originalError);
      } else {
        try {
          fallback.detectLang(filteredObjs);
          serviceId = fallback.getServiceId();
        } catch (LanguageDetectionException e) {
          if (logger.isDebugEnabled()) {
            logger.debug("Error when calling default service. ", e);
          }
          throwApiException(originalError);
        }
      }
    }
    return new LangDetectResponse(getResults(languageDetectionObjs), serviceId);
  }

  private List<String> getResults(List<LanguageDetectionObj> languageDetectionObjs) {
    return languageDetectionObjs.stream().map( obj -> (obj.getDetectedLang())).collect(Collectors.toList());
  }

  private LanguageDetectionService getFallbackService(LangDetectRequest langDetectRequest)
      throws ParamValidationException {
    // only if indicated in request
    if (langDetectRequest.getFallback() == null) {
      return null;
    }
    // call the fallback service in case of failed lang detection (non 200 response by remote
    // service)
    return getServiceInstance(langDetectRequest.getFallback(), langDetectRequest.getLang(), true);
  }

  private LanguageDetectionService getLangDetectService(LangDetectRequest langDetectRequest)
      throws ParamValidationException {
    final String requestedServiceId = langDetectRequest.getService();
    final String languageHint = langDetectRequest.getLang();

    if (requestedServiceId != null) {
      return getServiceInstance(requestedServiceId, languageHint);
    } else {
      final String defaultServiceId = translationServiceProvider.getTranslationServicesConfig()
          .getLangDetectConfig().getDefaultServiceId();
      return getServiceInstance(defaultServiceId, languageHint);
    }
  }

  private LanguageDetectionService getServiceInstance(final String requestedServiceId,
      final String languageHint) throws ParamValidationException {
    return getServiceInstance(requestedServiceId, languageHint, false);
  }

  private LanguageDetectionService getServiceInstance(final String requestedServiceId,
      final String languageHint, boolean isFallbackService) throws ParamValidationException {
    LanguageDetectionService detectService =
        translationServiceProvider.getLangDetectServices().get(requestedServiceId);
    if (detectService == null) {
      final String paramName =
          isFallbackService ? TranslationAppConstants.FALLBACK : TranslationAppConstants.SERVICE;
      final String availableServices =
          translationServiceProvider.getLangDetectServices().keySet().toString();
      throw new ParamValidationException("Requested service is invalid:" + requestedServiceId,
          ERROR_INVALID_PARAM_VALUE, ERROR_INVALID_PARAM_VALUE, new String[] {paramName,
              requestedServiceId + " (available services: " + availableServices + ")"});
    }
    // check if the "lang" is supported
    if (languageHint != null && !detectService.isSupported(languageHint)) {
      throw new ParamValidationException("Language hint not supported:" + languageHint,
          ERROR_INVALID_PARAM_VALUE, ERROR_UNSUPPORTED_LANG,
          new String[] {TranslationAppConstants.LANG, requestedServiceId});
    }
    return detectService;
  }

  public boolean isLangDetectionSupported(@NotNull String lang) {
    return translationServiceProvider.getTranslationServicesConfig().getLangDetectConfig()
        .getSupported().contains(lang.toLowerCase(Locale.ENGLISH));
  }

  private List<LanguageDetectionObj> buildLangDetectionObjectList(LangDetectRequest langDetectRequest) {
    // create a list of objects to be lang detected
    List<LanguageDetectionObj> detectionObjs = new ArrayList<LanguageDetectionObj>(langDetectRequest.getText().size());
    for (String inputText : langDetectRequest.getText()) {
      LanguageDetectionObj newLangDetectObj = new LanguageDetectionObj();
      // hint is optional
      if (langDetectRequest.getLang() != null) {
        newLangDetectObj.setHint(langDetectRequest.getLang());
      }
      newLangDetectObj.setText(inputText);
      newLangDetectObj.setTranslated(false); // not yet processed/translated
      detectionObjs.add(newLangDetectObj);
    }
    return detectionObjs;
  }

  @PreDestroy
  public void close() {
    // call close method of all detection services
    for (LanguageDetectionService service : translationServiceProvider.getLangDetectServices()
        .values()) {
      service.close();
    }
  }

}
