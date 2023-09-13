package eu.europeana.api.translation.web.service;

import java.util.List;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.translation.config.I18nConstants;
import eu.europeana.api.translation.config.TranslationServiceProvider;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

@Service
public class TranslationWebService {

  @Autowired
  private TranslationServiceProvider translationServiceProvider;
  
  private final Logger logger = LogManager.getLogger(getClass());
  
  public TranslationResponse translate(TranslationRequest translationRequest) throws Exception {
    LanguagePair languagePair =
        new LanguagePair(translationRequest.getSource(), translationRequest.getTarget());
    TranslationService translationService = selectTranslationService(translationRequest, languagePair);
    TranslationService fallback = null;
    if(translationRequest.getFallback() != null) {
      fallback = getTranslationService(translationRequest.getFallback(), languagePair);
    }
    
    List<String> translations = null;
    String serviceId = null;
    try {
      translations = translationService.translate(translationRequest.getText(), translationRequest.getTarget(), translationRequest.getSource());
      serviceId = translationService.getServiceId();
    } catch (TranslationException originalError) {
      // call the fallback service in case of failed translation
      if (fallback == null) {
        throw originalError;
      }
      
      try {
        translations = fallback.translate(translationRequest.getText(), translationRequest.getTarget(), translationRequest.getSource());
        serviceId = fallback.getServiceId();
      } catch(TranslationException e) {
        if(logger.isDebugEnabled()) {
          logger.debug("Error when calling default service. ", e);
        }
        //return original exception
        throw originalError;
      }
    }
    TranslationResponse result = new TranslationResponse();
    result.setTranslations(translations);
    result.setLang(translationRequest.getTarget());
    result.setService(serviceId);
    return result;
  }

  private TranslationService selectTranslationService(TranslationRequest translationRequest, LanguagePair languagePair)
      throws ParamValidationException {


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

  private TranslationService selectFromLanguageMappings(LanguagePair languagePair){
    final String key = LanguagePair.generateKey(languagePair.getSrcLang(), languagePair.getTargetLang());
    return translationServiceProvider.getLangMappings4TranslateServices().getOrDefault(key, null);  
  }

  private TranslationService getTranslationService(final String serviceId,
      LanguagePair languagePair) throws ParamValidationException{
    return getTranslationService(serviceId, languagePair, false);
  }
  private TranslationService getTranslationService(final String serviceId,
      LanguagePair languagePair, boolean fallback) throws ParamValidationException {
    TranslationService result =
        translationServiceProvider.getTranslationServices().get(serviceId);
    String param = fallback ? TranslationAppConstants.FALLBACK : TranslationAppConstants.SERVICE;
    if (result == null) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM,
          new String[] {param, serviceId + " (available services: " + String.join(", ", translationServiceProvider.getTranslationServices().keySet()) + ")"});
    }
    if (!result.isSupported(languagePair.getSrcLang(), languagePair.getTargetLang())) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM,
          new String[] {TranslationAppConstants.SOURCE_LANG + TranslationAppConstants.LANG_DELIMITER
              + TranslationAppConstants.TARGET_LANG, languagePair.toString()});
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
    //call close method of all translation services
    for (TranslationService service : translationServiceProvider.getTranslationServices().values()) {
      service.close(); 
    }
  }
}
