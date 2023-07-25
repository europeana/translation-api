package eu.europeana.api.translation.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.translation.config.I18nConstants;
import eu.europeana.api.translation.config.InitServicesGlobalJsonConfig;
import eu.europeana.api.translation.config.serialization.TranslationLangPairCfg;
import eu.europeana.api.translation.config.serialization.TranslationMappingCfg;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;

@Service
public class TranslationServiceImpl {

  @Autowired
  private InitServicesGlobalJsonConfig initGlobalJsonConfig;

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
  
  public LangDetectResponse detectLang(LangDetectRequest langDetectRequest) throws Exception {
    LanguageDetectionService langDetectService = getLangDetectService(langDetectRequest);
    List<String> langs = null;
    try {
      langs = langDetectService.detectLang(langDetectRequest.getText(), langDetectRequest.getLang()); 
    }
    catch (Exception ePrimary) {
      //call the fallback service in case of failed lang detection
      LanguageDetectionService fallback = getLangDetectFallbackService(langDetectRequest);
      if(fallback==null) {
        throw ePrimary;
      }
      try {
        langs = fallback.detectLang(langDetectRequest.getText(), langDetectRequest.getLang());
      }
      catch (Exception eFallback) {
        throw ePrimary;
      }
    }
    
    LangDetectResponse result = new LangDetectResponse();
    result.setLangs(langs);
    result.setLang(langDetectRequest.getLang());
    return result;
  }  
   
  private TranslationService getTranslFallbackService(TranslationRequest translRequest) throws ParamValidationException {
    if(translRequest.getFallback()==null) {
      return null;
    }
    TranslationService fallback = initGlobalJsonConfig.getTranslServices().get(translRequest.getFallback());
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
    List<TranslationLangPairCfg> langPairCfgList = initGlobalJsonConfig.getAppGlobalJsonConfig().getTranslConfig().getSupported();
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
      TranslationService result = initGlobalJsonConfig.getTranslServices().get(translRequest.getService());
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
      for(TranslationMappingCfg translMappingCfg : initGlobalJsonConfig.getAppGlobalJsonConfig().getTranslConfig().getMappings()) {
        if(translMappingCfg.getSrcLang().contains(translRequest.getSource()) && translMappingCfg.getTrgLang().contains(translRequest.getTarget())) {
          return initGlobalJsonConfig.getTranslServices().get(translMappingCfg.getServiceId());
        }
      }
      //otherwise choose the default service
      return initGlobalJsonConfig.getTranslServices().get(initGlobalJsonConfig.getAppGlobalJsonConfig().getTranslConfig().getDefaultServiceId());
    }
  }

  private LanguageDetectionService getLangDetectFallbackService(LangDetectRequest langDetectRequest) throws ParamValidationException {
    if(langDetectRequest.getFallback()==null) {
      return null;
    }
    LanguageDetectionService fallback = initGlobalJsonConfig.getLangDetectServices().get(langDetectRequest.getFallback());
    if(fallback==null) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.FALLBACK, langDetectRequest.getFallback()});
    }
    if(langDetectRequest.getLang()!=null && !fallback.isSupported(langDetectRequest.getLang())) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.LANG, langDetectRequest.getLang()});
    }
    return fallback;
  }

  private LanguageDetectionService getLangDetectService(LangDetectRequest langDetectRequest) throws ParamValidationException {
    //check if "lang" from the request is supported
    if(langDetectRequest.getLang()!=null && !initGlobalJsonConfig.getAppGlobalJsonConfig().getLangDetectConfig().getSupported().contains(langDetectRequest.getLang())) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.LANG, langDetectRequest.getLang()});
    }
    //get the right lang detect service
    if(langDetectRequest.getService() != null) {
      LanguageDetectionService result = initGlobalJsonConfig.getLangDetectServices().get(langDetectRequest.getService());
      if(result==null) {
        throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SERVICE, langDetectRequest.getService()});
      }
      //check if the "lang" is supported
      if(langDetectRequest.getLang()!=null && !result.isSupported(langDetectRequest.getLang())) {
        throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.LANG, langDetectRequest.getLang()});
      }
      return result;
    }
    else {
      return initGlobalJsonConfig.getLangDetectServices().get(initGlobalJsonConfig.getAppGlobalJsonConfig().getLangDetectConfig().getDefaultServiceId());
    }
  }

}
