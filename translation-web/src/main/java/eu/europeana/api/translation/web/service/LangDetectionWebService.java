package eu.europeana.api.translation.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons_sb3.error.EuropeanaI18nApiException;
import eu.europeana.api.commons_sb3.error.exceptions.InvalidParamException;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.config.TranslationServiceProvider;
import eu.europeana.api.translation.definitions.model.LangDetectRequest;
import eu.europeana.api.translation.definitions.model.LangDetectResponse;
import eu.europeana.api.translation.definitions.model.LanguageDetectionObj;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.exception.LanguageDetectionException;

@Service
public class LangDetectionWebService extends BaseWebService {

  @Resource(name = BeanNames.BEAN_SERVICE_PROVIDER)
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
      if(logger.isDebugEnabled()) {
        logger.debug("Requesting lang detection from service: {}, for text: {}", langDetectService.getServiceId(), 
            filteredObjs.stream().map(to -> to.getText()).toList());
      }
      
      langDetectService.detectLang(filteredObjs);
      serviceId = langDetectService.getServiceId();
    } catch (LanguageDetectionException originalError) {
      // check if fallback is available
      if (fallback == null) {
        throwApiException(originalError);
      } else {
        try {
          logger.debug("Requesting lang detection from falback service: {}, for text: {}", fallback.getServiceId(), 
              filteredObjs.stream().map(to -> to.getText()).toList());
          
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
      throws InvalidParamException {
    // only if indicated in request
    if (langDetectRequest.getFallback() == null) {
      return null;
    }
    // call the fallback service in case of failed lang detection (non 200 response by remote
    // service)
    return getServiceInstance(langDetectRequest.getFallback(), langDetectRequest.getLang(), true);
  }

  private LanguageDetectionService getLangDetectService(LangDetectRequest langDetectRequest)
      throws InvalidParamException {
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
      final String languageHint) throws InvalidParamException {
    return getServiceInstance(requestedServiceId, languageHint, false);
  }

  private LanguageDetectionService getServiceInstance(final String requestedServiceId,
      final String languageHint, boolean isFallbackService) throws InvalidParamException {
    LanguageDetectionService detectService =
        translationServiceProvider.getLangDetectionService(requestedServiceId);
    if (detectService == null) {
      final String paramName =
          isFallbackService ? TranslationAppConstants.FALLBACK : TranslationAppConstants.SERVICE;
      final String availableServices =
          translationServiceProvider.getAvailableLangDetectionServiceIds().toString();
      throw new InvalidParamException (List.of(paramName,
              requestedServiceId + " (available services: " + availableServices + ")"));
    }
    // check if the "lang" is supported
    if (languageHint != null && !detectService.isSupported(languageHint)) {
      throw new InvalidParamException ((List.of(TranslationAppConstants.LANG, requestedServiceId)));
    }
    return detectService;
  }

  public boolean isLangDetectionSupported(@NonNull String lang) {
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
