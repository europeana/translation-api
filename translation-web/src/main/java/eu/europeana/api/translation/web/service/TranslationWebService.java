package eu.europeana.api.translation.web.service;

import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_INVALID_PARAM_VALUE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;
import eu.europeana.api.translation.config.TranslationServiceProvider;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
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
  
  public TranslationResponse translate(TranslationRequest translationRequest) throws EuropeanaI18nApiException {
    if(translationRequest.useCaching() && isCachingEnabled()) {
      //only if the request requires caching and the caching service is enabled
      return getCombinedCachedAndTranslatedResults(translationRequest);
    }
    else {
      return getTranslatedResults(translationRequest);
    }
  }
  
  private boolean isCachingEnabled() {
    return getRedisCacheService() != null;
  }

  private TranslationResponse getTranslatedResults(TranslationRequest translationRequest) throws EuropeanaI18nApiException {
    LanguagePair languagePair =
        new LanguagePair(translationRequest.getSource(), translationRequest.getTarget());
    TranslationService translationService =
        selectTranslationService(translationRequest, languagePair);
    TranslationService fallback = null;
    if (translationRequest.getFallback() != null) {
      fallback = getTranslationService(translationRequest.getFallback(), languagePair, true);
    }

    List<String> translations = null;
    String serviceId = null;
    try {
      translations = translationService.translate(translationRequest.getText(),
          translationRequest.getTarget(), translationRequest.getSource());
      serviceId = translationService.getServiceId();
    } catch (TranslationException originalError) {
      // call the fallback service in case of failed translation
      if (fallback == null) {
        throwApiException(originalError);
      } else {
        try {
          translations = fallback.translate(translationRequest.getText(),
              translationRequest.getTarget(), translationRequest.getSource());
          serviceId = fallback.getServiceId();
        } catch (TranslationException e) {
          if (logger.isDebugEnabled()) {
            logger.debug("Error when calling default service. ", e);
          }
          // return original exception
          throwApiException(originalError);
        }
      }
    }
    TranslationResponse result = new TranslationResponse();
    result.setTranslations(translations);
    result.setLang(translationRequest.getTarget());
    result.setService(serviceId);
    return result;
  }
  
  private TranslationResponse getCombinedCachedAndTranslatedResults(TranslationRequest translRequest) throws EuropeanaI18nApiException {
    TranslationResponse result=null;
    List<String> redisResp = getRedisCacheService().getCachedTranslations(translRequest.getSource(), translRequest.getTarget(), translRequest.getText());
    if(Collections.frequency(redisResp, null)>0) {
      
      TranslationRequest newTranslReq = new TranslationRequest();
      newTranslReq.setSource(translRequest.getSource());
      newTranslReq.setTarget(translRequest.getTarget());
      newTranslReq.setService(translRequest.getService());
      newTranslReq.setFallback(translRequest.getFallback());
      newTranslReq.setCaching(translRequest.getCaching());
      newTranslReq.setText(new ArrayList<>(translRequest.getText()));

      List<String> newText = new ArrayList<>();
      int counter=0;
      for(String redisRespElem : redisResp) {
        if(redisRespElem==null) {
          newText.add(translRequest.getText().get(counter));
          counter++;
        }
      }
      newTranslReq.setText(newText);
      result = getTranslatedResults(newTranslReq);
      
      //save the translations to the cache
      getRedisCacheService().saveRedisCache(newTranslReq.getSource(), newTranslReq.getTarget(), newTranslReq.getText(), result.getTranslations());
      
      //aggregate the redis and translation responses
      List<String> finalText=new ArrayList<>(redisResp);
      int counterTranslated = 0;
      for(int i=0;i<finalText.size();i++) {
        if(finalText.get(i)==null) {
          finalText.set(i, result.getTranslations().get(counterTranslated));
          counterTranslated++;
        }
      }
      result.setService(null);
      result.setTranslations(finalText);       
    }
    else {
      result=new TranslationResponse();
      result.setLang(translRequest.getTarget());
      result.setTranslations(redisResp);
    }

    return result;
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
