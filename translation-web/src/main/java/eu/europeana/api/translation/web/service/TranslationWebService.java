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
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.config.TranslationServiceProvider;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.etranslation.ETranslationTranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;
import eu.europeana.api.translation.service.util.TranslationUtils;
import eu.europeana.api.translation.web.exception.ParamValidationException;

@Service
public class TranslationWebService extends BaseWebService {
  
  @Autowired protected TranslationConfig translationConfig;

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
    // pre processing for translation
    try {
      translationServiceProvider.getTranslationServicePreProcessor().translate(translObjs);
    } catch (TranslationException e) {
     logger.error("Error during the pre processing ", e);
    }
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
        serviceId = cachedTranslationService.getServiceId();
        // send the values which are not yet translated (isTranslated=false) for the translations
        cachedTranslationService.translate(translObjs.stream().filter(to -> !to.isTranslated()).collect(Collectors.toList()));
        // update service ID after the translate() method, because the serviceId may change (depending if there is sth in the cache)
        //NOTE: is this really needed?
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
    if(shouldTruncateText(translationRequest)) {
      limitTextSizeForETranslation(translationRequest, translObjs);
    } else {
      fillTranslationObjects(translObjs, translationRequest);
    }
    return translObjs;
  }
  
  private void fillTranslationObjects(List<TranslationObj> translationObjects,
      TranslationRequest translationRequest) {
    //when we do not need the above method limitTextSizeForETranslationStressTest, leave just this for loop (as was before)
    final String source = translationRequest.getSource();
    final String target = translationRequest.getTarget();
    
    TranslationObj newTranslObj;
    for (String inputText : translationRequest.getText()) {
      newTranslObj = buildTranslationObject(source, target, inputText);
      translationObjects.add(newTranslObj);
    }
  }

  private TranslationObj buildTranslationObject(final String source, final String target,
      String inputText) {
    TranslationObj newTranslObj = new TranslationObj();
    newTranslObj.setSourceLang(source);
    newTranslObj.setTargetLang(target);
    newTranslObj.setText(inputText);
    newTranslObj.setTranslated(false); // not translated yet hence set to false
    return newTranslObj;
  }
  
  /*
   * This method is used only for the purpose of eTranslation stress test and can be excluded afterwards
   */
  private void limitTextSizeForETranslation(TranslationRequest translationRequest, List<TranslationObj> translationObjects) {
    StringBuilder translJointString = new StringBuilder(TranslationUtils.STRING_BUILDER_INIT_SIZE);
    
    TranslationObj newTranslObj;
    
    for (String inputText : translationRequest.getText()) {
        
       //append delimiter if needed and doesn't exceed the limit 
       if(!translJointString.isEmpty() && !exceedesSnippetLimit(translJointString, ETranslationTranslationService.MARKUP_DELIMITER)) {
          //append new paragraph delimiter
          translJointString.append(ETranslationTranslationService.MARKUP_DELIMITER);
        } else if(exceedesSnippetLimit(translJointString, ETranslationTranslationService.MARKUP_DELIMITER)){
          //cannot add more text as limit will exceed, if the delimiter is appended
          //stop if delimiter cannot be appended
          break;
        }
        
        if(!exceedesSnippetLimit(translJointString, inputText)) {
          //no truncation needed
          //append to joint string
          translJointString.append(inputText);
          //add to translation objects
          newTranslObj = buildTranslationObject(translationRequest.getSource(), translationRequest.getTarget(), inputText);
          translationObjects.add(newTranslObj);
         } else {
          //truncation is needed
          final int charsAvailableForSnippet = ETranslationTranslationService.ETRANSLATION_SNIPPET_LIMIT - translJointString.length();
          //ensure end index is smaller or equal than the length of the text
          //TODO: eventually ensure to break at last space (or punctuation) char
          final int lastCharIndex = Math.min(charsAvailableForSnippet, inputText.length());
          String truncatedInput=inputText.substring(0, lastCharIndex);
          
          //append to joint string
          translJointString.append(truncatedInput);
          
          //add to translation objects
          newTranslObj = buildTranslationObject(translationRequest.getSource(), translationRequest.getTarget(), truncatedInput);
          translationObjects.add(newTranslObj);
          
          //stop after truncated text
          break;
        }
      }
  }

  private boolean exceedesSnippetLimit(StringBuilder translJointString, String textToAppend) {
    return translJointString.length() + textToAppend.length() >= ETranslationTranslationService.ETRANSLATION_SNIPPET_LIMIT;
  }

  private boolean shouldTruncateText(TranslationRequest translationRequest) {
    return translationConfig.isEtranslationTruncate()
        && ETranslationTranslationService.DEFAULT_SERVICE_ID.equals(translationRequest.getService());
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
