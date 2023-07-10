package eu.europeana.api.translation.web.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.translation.config.InitServicesGlobalJsonConfig;
import eu.europeana.api.translation.config.serialization.LangDetectRequestJsonConfig;
import eu.europeana.api.translation.config.serialization.LangDetectResponseJsonConfig;
import eu.europeana.api.translation.config.serialization.TranslationGlobalJsonConfig;
import eu.europeana.api.translation.config.serialization.TranslationRequestJsonConfig;
import eu.europeana.api.translation.config.serialization.TranslationResponseJsonConfig;
import eu.europeana.api.translation.web.exception.TranslationException;

@Service
public class TranslationServiceImpl {

  @Autowired
  private InitServicesGlobalJsonConfig initGlobalJsonConfig;
  
  public TranslationGlobalJsonConfig info() {
    return initGlobalJsonConfig.getAppGlobalJsonConfig();
  }
  
  public TranslationResponseJsonConfig translate(TranslationRequestJsonConfig translRequest) throws TranslationException {
    TranslationService defaultTranslService = getDefaultTranslService();
    List<String> translations = defaultTranslService.translate(translRequest.getText(), translRequest.getTarget(), translRequest.getSource(), translRequest.getDetect());
    TranslationResponseJsonConfig result = new TranslationResponseJsonConfig();
    result.setTranslations(translations);
    result.setLang(translRequest.getTarget());
    return result;
  }
  
  public LangDetectResponseJsonConfig detectLang(LangDetectRequestJsonConfig langDetectRequest) throws TranslationException {
    LanguageDetectionService defaultLangDetectService = getDefaultLangDetectService();
    List<String> langs = defaultLangDetectService.detectLang(langDetectRequest.getText(), langDetectRequest.getLang()); 
    LangDetectResponseJsonConfig result = new LangDetectResponseJsonConfig();
    result.setLangs(langs);
    result.setLang(langDetectRequest.getLang());
    return result;
  }  
  
  private TranslationService getDefaultTranslService() throws TranslationException {
    List<TranslationService> translServices = initGlobalJsonConfig.getTranslServices();
    String defaultTranslServiceClassname = initGlobalJsonConfig.getAppGlobalJsonConfig().getTranslConfig().getDefaultClassname();
    for(TranslationService translServ : translServices) {
      if(defaultTranslServiceClassname.equals(translServ.getClass().getName())) {
        return translServ;
      }
    }
    throw new TranslationException("There is no default TranslationService configured.");
  }
  
  private LanguageDetectionService getDefaultLangDetectService() throws TranslationException {
    List<LanguageDetectionService> langDetestServices = initGlobalJsonConfig.getLangDetectServices();
    String defaultLangDetectServiceClassname = initGlobalJsonConfig.getAppGlobalJsonConfig().getLangDetectConfig().getDefaultClassname();
    for(LanguageDetectionService langDetectServ : langDetestServices) {
      if(defaultLangDetectServiceClassname.equals(langDetectServ.getClass().getName())) {
        return langDetectServ;
      }
    }
    throw new TranslationException("There is no default LanguageDetectionService configured.");
  }

}
