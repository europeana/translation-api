package eu.europeana.api.translation.web.service;

import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_INVALID_PARAM_VALUE;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;
import eu.europeana.api.translation.config.TranslationServiceProvider;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.web.exception.ParamValidationException;

@Service
public class TranslationWebService extends BaseWebService {

  @Autowired
  private final TranslationServiceProvider translationServiceProvider;

  private RedisCacheService redisCacheService;

  private final Logger logger = LogManager.getLogger(getClass());

  @Autowired
  public TranslationWebService(TranslationServiceProvider translationServiceProvider) {
    this.translationServiceProvider = translationServiceProvider;
  }

  public TranslationResponse translate(TranslationRequest translationRequest)
      throws EuropeanaI18nApiException {
    List<TranslationObj> translObjs = buildTranslationObjectList(translationRequest);

    // get the configured translation services
    LanguagePair languagePair =
        new LanguagePair(translationRequest.getSource(), translationRequest.getTarget());
    TranslationService translationService =
        selectTranslationService(translationRequest, languagePair);
    TranslationService fallback = null;
    if (translationRequest.getFallback() != null) {
      fallback = getTranslationService(translationRequest.getFallback(), languagePair, true);
    }

    // build the list of caching services
    List<TranslationService> cachedTranslationServices = buildCachedTranslationServices(
        translationRequest.useCaching(), translationService, fallback);

    // calling the translation services and creating the results
    TranslationException translationError = null;
    String serviceId = null;
    for (TranslationService cachedTranslationService : cachedTranslationServices) {
      try {
        cachedTranslationService.translate(translObjs);
        // call this method after the translate() method, because the serviceId changes depending if
        // there is sth in the cache
        serviceId = cachedTranslationService.getServiceId();
        // clear translation error if the invocation is successfull
        translationError = null;
        break;
      } catch (TranslationException ex) {
        // keep the original exception for error response
        if (translationError == null) {
          translationError = ex;
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Error when calling translation service: " + serviceId, ex);
        }
      }
    }

    if (translationError != null) {
      throwApiException(translationError);
    }

    return buildTranslationResponse(translationRequest, translObjs, serviceId);
  }


  private TranslationResponse buildTranslationResponse(TranslationRequest translationRequest,
      List<TranslationObj> translObjs, String serviceId) {
    TranslationResponse result = new TranslationResponse();
    result.setTranslations(
        translObjs.stream().map(el -> el.getTranslation()).collect(Collectors.toList()));
    result.setLang(translationRequest.getTarget());
    result.setService(serviceId);
    return result;
  }

  private List<TranslationService> buildCachedTranslationServices(boolean useCaching,
      TranslationService translationService, TranslationService fallback) {
    List<TranslationService> cachedTranslationServices = new ArrayList<TranslationService>();
    // if(translationRequest.useCaching() && isCachingEnabled()) {
    cachedTranslationServices
        .add(instantiateCachedTranslationService(useCaching, translationService));

    if (fallback != null) {
      cachedTranslationServices.add(instantiateCachedTranslationService(useCaching, fallback));
    }
    // } else {
    // translServicesToCall.add(translationService);
    // if(fallback!=null) {
    // translServicesToCall.add(fallback);
    // }
    // }
    return cachedTranslationServices;
  }

  CachedTranslationService instantiateCachedTranslationService(boolean useCaching,
      TranslationService translationService) {
    if (useCaching) {
      return new CachedTranslationService(redisCacheService, translationService);
    } else {
      return new CachedTranslationService(null, translationService);
    }
  }

  private List<TranslationObj> buildTranslationObjectList(TranslationRequest translationRequest) {
    // create a list of objects to be translated
    List<TranslationObj> translObjs = new ArrayList<TranslationObj>(translationRequest.getText().size());
    for (String inputText : translationRequest.getText()) {
      TranslationObj newTranslObj = new TranslationObj();
      newTranslObj.setSourceLang(translationRequest.getSource());
      newTranslObj.setTargetLang(translationRequest.getTarget());
      newTranslObj.setText(inputText);
      translObjs.add(newTranslObj);
    }
    return translObjs;
  }

  private TranslationService selectTranslationService(TranslationRequest translationRequest,
      LanguagePair languagePair) throws ParamValidationException {
    final String serviceId = translationRequest.getService();
    if (serviceId != null) {
      // get the translation service by id
      return getTranslationService(serviceId, languagePair);
    } else if (languagePair.getSrcLang() != null) {
      // search in language mappings
      TranslationService translationService = selectFromLanguageMappings(languagePair);
      if (translationService != null) {
        return translationService;
      }
    }

    // if none selected pick the default
    final String defaultServiceId = translationServiceProvider.getTranslationServicesConfig()
        .getTranslationConfig().getDefaultServiceId();

    return getTranslationService(defaultServiceId, languagePair);
  }

  private TranslationService selectFromLanguageMappings(LanguagePair languagePair) {
    final String key =
        LanguagePair.generateKey(languagePair.getSrcLang(), languagePair.getTargetLang());
    return translationServiceProvider.getLangMappings4TranslateServices().getOrDefault(key, null);
  }

  private TranslationService getTranslationService(final String serviceId,
      LanguagePair languagePair) throws ParamValidationException {
    return getTranslationService(serviceId, languagePair, false);
  }

  private TranslationService getTranslationService(final String serviceId,
      LanguagePair languagePair, boolean fallback) throws ParamValidationException {
    TranslationService result = translationServiceProvider.getTranslationServices().get(serviceId);
    String param = fallback ? TranslationAppConstants.FALLBACK : TranslationAppConstants.SERVICE;
    if (result == null) {
      throw new ParamValidationException("Requested service id is invalid" + serviceId,
          ERROR_INVALID_PARAM_VALUE, ERROR_INVALID_PARAM_VALUE,
          new String[] {param,
              serviceId + " (available services: "
                  + String.join(", ", translationServiceProvider.getTranslationServices().keySet())
                  + ")"});
    }
    if (!result.isSupported(languagePair.getSrcLang(), languagePair.getTargetLang())) {
      throw new ParamValidationException("Language pair not supported:" + languagePair,
          ERROR_INVALID_PARAM_VALUE, ERROR_INVALID_PARAM_VALUE,
          new String[] {LanguagePair.generateKey(TranslationAppConstants.SOURCE_LANG,
              TranslationAppConstants.TARGET_LANG), languagePair.toString()});
    }
    return result;
  }

  public boolean isTranslationSupported(LanguagePair languagePair) {
    // check if the "source" and "target" params are supported
    List<TranslationLangPairCfg> langPairCfgList = translationServiceProvider
        .getTranslationServicesConfig().getTranslationConfig().getSupported();
    if (languagePair.getSrcLang() == null) {
      return isTargetInList(languagePair.getTargetLang(), langPairCfgList);
    }

    return isLangPairInList(languagePair, langPairCfgList);
  }

  private boolean isLangPairInList(LanguagePair languagePair,
      List<TranslationLangPairCfg> langPairCfgList) {
    for (TranslationLangPairCfg langPairCfg : langPairCfgList) {
      if (langPairCfg.getSrcLang().contains(languagePair.getSrcLang())
          && langPairCfg.getTargetLang().contains(languagePair.getTargetLang())) {
        return true;
      }
    }
    return false;
  }

  private boolean isTargetInList(String targetLang, List<TranslationLangPairCfg> langPairCfgList) {
    for (TranslationLangPairCfg translationLangPairCfg : langPairCfgList) {
      if (translationLangPairCfg.getTargetLang().contains(targetLang)) {
        return true;
      }
    }
    return false;
  }

  @PreDestroy
  public void close() {
    // call close method of all translation services
    for (TranslationService service : translationServiceProvider.getTranslationServices()
        .values()) {
      service.close();
    }
  }

  public RedisCacheService getRedisCacheService() {
    return redisCacheService;
  }

  @Autowired(required = false)
  public void setRedisCacheService(RedisCacheService redisCacheService) {
    this.redisCacheService = redisCacheService;
  }
}
