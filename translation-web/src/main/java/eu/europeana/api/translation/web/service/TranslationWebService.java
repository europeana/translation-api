package eu.europeana.api.translation.web.service;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.translation.config.I18nConstants;
import eu.europeana.api.translation.config.TranslationServiceConfigProvider;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.config.services.TranslationMappingCfg;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.TranslationService;

@Service
public class TranslationWebService {

  @Autowired
  private TranslationServiceConfigProvider translationServiceConfigProvider;

  public TranslationResponse translate(TranslationRequest translRequest) throws Exception {
    TranslationService translService = getTranslService(translRequest);
    List<String> translations = null;
    try {
      translations = translService.translate(translRequest.getText(), translRequest.getTarget(), translRequest.getSource(), translRequest.getDetect());
    }
    catch (Exception ePrimary) {
      //call the fallback service in case of failed translation
      TranslationService fallback = getTranslFallbackService(translRequest); 
      if(fallback==null) {
        throw ePrimary;
      }
      try {
        translations = fallback.translate(translRequest.getText(), translRequest.getTarget(), translRequest.getSource(), translRequest.getDetect());
      }
      catch (Exception eFallback) {
        throw ePrimary;
      }
    }
    TranslationResponse result = new TranslationResponse();
    result.setTranslations(translations);
    result.setLang(translRequest.getTarget());
    return result;
  }
  
  private TranslationService getTranslFallbackService(TranslationRequest translRequest) throws ParamValidationException {
    if(translRequest.getFallback()==null) {
      return null;
    }
    TranslationService fallback = translationServiceConfigProvider.getTranslationServices().get(translRequest.getFallback());
    if(fallback==null) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.FALLBACK, translRequest.getFallback()});
    }
    if(! fallback.isSupported(translRequest.getSource(), translRequest.getTarget())) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SOURCE_LANG + TranslationAppConstants.LANG_DELIMITER + TranslationAppConstants.TARGET_LANG, translRequest.getSource() + TranslationAppConstants.LANG_DELIMITER + translRequest.getTarget()});
    }
    return fallback;
  }
  
  private TranslationService getTranslService(TranslationRequest translRequest) throws ParamValidationException {
    //check if the "source" and "target" params are supported 
    List<TranslationLangPairCfg> langPairCfgList = translationServiceConfigProvider.getTranslationServicesConfig().getTranslationConfig().getSupported();
    boolean langPairSupported = false;
    for(TranslationLangPairCfg langPairCfg : langPairCfgList) {
      if(langPairCfg.getSrcLang().contains(translRequest.getSource()) && langPairCfg.getTrgLang().contains(translRequest.getTarget())) {
        langPairSupported=true;
        break;
      }
    }
    if(! langPairSupported) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SOURCE_LANG + TranslationAppConstants.LANG_DELIMITER + TranslationAppConstants.TARGET_LANG, translRequest.getSource() + TranslationAppConstants.LANG_DELIMITER + translRequest.getTarget()});
    }
    //get the right transl service
    if(translRequest.getService() != null) {
      TranslationService result = translationServiceConfigProvider.getTranslationServices().get(translRequest.getService());
      if(result==null) {
        throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SERVICE, translRequest.getService()});
      }
      if(! result.isSupported(translRequest.getSource(), translRequest.getTarget())) {
        throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SOURCE_LANG + TranslationAppConstants.LANG_DELIMITER + TranslationAppConstants.TARGET_LANG, translRequest.getSource() + TranslationAppConstants.LANG_DELIMITER + translRequest.getTarget()});
      }
      return result;
    }
    else {
      //check if the src and trg langs are in the mappings and choose that service
      for(TranslationMappingCfg translMappingCfg : translationServiceConfigProvider.getTranslationServicesConfig().getTranslationConfig().getMappings()) {
        if(translMappingCfg.getSrcLang().contains(translRequest.getSource()) && translMappingCfg.getTrgLang().contains(translRequest.getTarget())) {
          return translationServiceConfigProvider.getTranslationServices().get(translMappingCfg.getServiceId());
        }
      }
      //otherwise choose the default service
      return translationServiceConfigProvider.getTranslationServices().get(translationServiceConfigProvider.getTranslationServicesConfig().getTranslationConfig().getDefaultServiceId());
    }
  }
  
  public boolean isTranslationSupported(LanguagePair LangugePair) {
    return false;
  }
}
