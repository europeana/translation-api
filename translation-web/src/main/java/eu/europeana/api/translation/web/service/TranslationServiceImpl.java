package eu.europeana.api.translation.web.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.translation.config.I18nConstants;
import eu.europeana.api.translation.config.InitServicesGlobalJsonConfig;
import eu.europeana.api.translation.config.serialization.DetectServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationServicesConfiguration;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.web.exception.TranslationException;

@Service
public class TranslationServiceImpl {

  @Autowired
  private InitServicesGlobalJsonConfig initGlobalJsonConfig;
  
  public TranslationServicesConfiguration info() {
    return initGlobalJsonConfig.getAppGlobalJsonConfig();
  }
  
  public TranslationResponse translate(TranslationRequest translRequest) throws ParamValidationException, TranslationException {
    TranslationService defaultTranslService = getTranslService(translRequest);
    List<String> translations = defaultTranslService.translate(translRequest.getText(), translRequest.getTarget(), translRequest.getSource(), translRequest.getDetect());
    TranslationResponse result = new TranslationResponse();
    result.setTranslations(translations);
    result.setLang(translRequest.getTarget());
    return result;
  }
  
  public LangDetectResponse detectLang(LangDetectRequest langDetectRequest) throws ParamValidationException, TranslationException {
    LanguageDetectionService langDetectService = getLangDetectService(langDetectRequest);
    List<String> langs = langDetectService.detectLang(langDetectRequest.getText(), langDetectRequest.getLang()); 
    LangDetectResponse result = new LangDetectResponse();
    result.setLangs(langs);
    result.setLang(langDetectRequest.getLang());
    return result;
  }  
    
  private TranslationService getTranslService(TranslationRequest translRequest) throws ParamValidationException {
    if(translRequest.getService() != null) {
      List<TranslationServiceCfg> translServCfgList = initGlobalJsonConfig.getAppGlobalJsonConfig().getTranslConfig().getServices();
      for(TranslationServiceCfg translServCfg : translServCfgList) {
        if(translServCfg.getId().equals(translRequest.getService())) {
          TranslationService result = getTranslationServiceBean(translServCfg.getClassname());
          //check if the source and target languages are supported
          if(! result.isSupported(translRequest.getSource(), translRequest.getTarget())) {
            throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SOURCE_LANG + TranslationAppConstants.LANG_DELIMITER + TranslationAppConstants.TARGET_LANG, translRequest.getSource() + TranslationAppConstants.LANG_DELIMITER + translRequest.getTarget()});
          }
          return result;
        }
      }
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SERVICE_ID, translRequest.getService()});
    }
    else {
      List<TranslationService> translServices = initGlobalJsonConfig.getTranslServices();
      for(TranslationService translServ : translServices) {
        if(translServ.isSupported(translRequest.getSource(), translRequest.getTarget())) {
          return translServ;
        }
      }
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SOURCE_LANG + TranslationAppConstants.LANG_DELIMITER + TranslationAppConstants.TARGET_LANG, translRequest.getSource() + TranslationAppConstants.LANG_DELIMITER + translRequest.getTarget()});
    }
  }
  
  private TranslationService getTranslationServiceBean(String translServiceClassname) throws ParamValidationException {
    List<TranslationService> translServices = initGlobalJsonConfig.getTranslServices();
    for(TranslationService translServ : translServices) {
      if(translServiceClassname.equals(translServ.getClass().getName())) {
        return translServ;
      }
    }
    throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_CONFIG_PARAM, new String[] {TranslationAppConstants.CLASSNAME, translServiceClassname});
  }

  private LanguageDetectionService getLangDetectServiceBean(String langDetectServiceClassname) throws ParamValidationException {
    List<LanguageDetectionService> langDetectServices = initGlobalJsonConfig.getLangDetectServices();
    for(LanguageDetectionService langDetectServ : langDetectServices) {
      if(langDetectServiceClassname.equals(langDetectServ.getClass().getName())) {
        return langDetectServ;
      }
    }
    throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_CONFIG_PARAM, new String[] {TranslationAppConstants.CLASSNAME, langDetectServiceClassname});
  }

  private LanguageDetectionService getLangDetectService(LangDetectRequest langDetectRequest) throws ParamValidationException {
    if(langDetectRequest.getService() != null) {
      List<DetectServiceCfg> detectServCfgList = initGlobalJsonConfig.getAppGlobalJsonConfig().getLangDetectConfig().getServices();
      for(DetectServiceCfg detectServCfg : detectServCfgList) {
        if(detectServCfg.getId().equals(langDetectRequest.getService())) {
          LanguageDetectionService result = getLangDetectServiceBean(detectServCfg.getClassname());
          //check if the source and target languages are supported
          if(langDetectRequest.getLang()!=null && !result.isSupported(langDetectRequest.getLang())) {
            throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.LANG, langDetectRequest.getLang()});
          }
          return result;
        }
      }
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.SERVICE, langDetectRequest.getService()});
    }
    else {
      if(langDetectRequest.getLang()!=null) {
        List<LanguageDetectionService> detectServices = initGlobalJsonConfig.getLangDetectServices();
        for(LanguageDetectionService detectServ : detectServices) {
          if(detectServ.isSupported(langDetectRequest.getLang())) {
            return detectServ;
          }
        }
        throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.LANG, langDetectRequest.getLang()});
      }
      else {
        List<DetectServiceCfg> detectServCfgList = initGlobalJsonConfig.getAppGlobalJsonConfig().getLangDetectConfig().getServices();
        for(DetectServiceCfg detectServCfg : detectServCfgList) {
          if(detectServCfg.getId().equals(initGlobalJsonConfig.getAppGlobalJsonConfig().getLangDetectConfig().getDefaultServiceId())) {
            return getLangDetectServiceBean(detectServCfg.getClassname());
          } 
        }
        throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_CONFIG_PARAM, new String[] {TranslationAppConstants.DEFAULT_SERVICE_ID, initGlobalJsonConfig.getAppGlobalJsonConfig().getLangDetectConfig().getDefaultServiceId()});
      }
    }
  }

}
