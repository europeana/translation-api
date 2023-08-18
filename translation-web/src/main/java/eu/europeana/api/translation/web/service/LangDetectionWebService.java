package eu.europeana.api.translation.web.service;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.translation.config.I18nConstants;
import eu.europeana.api.translation.config.TranslationServiceConfigProvider;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.service.LanguageDetectionService;

@Service
public class LangDetectionWebService {

  @Autowired
  private TranslationServiceConfigProvider translationServiceConfigProvider;

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
   
  
  private LanguageDetectionService getLangDetectFallbackService(LangDetectRequest langDetectRequest) throws ParamValidationException {
    if(langDetectRequest.getFallback()==null) {
      return null;
    }
    LanguageDetectionService fallback = translationServiceConfigProvider.getLangDetectServices().get(langDetectRequest.getFallback());
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
    if(langDetectRequest.getLang()!=null && !translationServiceConfigProvider.getTranslationServicesConfig().getLangDetectConfig().getSupported().contains(langDetectRequest.getLang())) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM, new String[] {TranslationAppConstants.LANG, langDetectRequest.getLang()});
    }
    //get the right lang detect service
    if(langDetectRequest.getService() != null) {
      LanguageDetectionService result = translationServiceConfigProvider.getLangDetectServices().get(langDetectRequest.getService());
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
      return translationServiceConfigProvider.getLangDetectServices().get(translationServiceConfigProvider.getTranslationServicesConfig().getLangDetectConfig().getDefaultServiceId());
    }
  }

  public boolean isLangDetectionSupported(@NotNull String lang) {
    return translationServiceConfigProvider.getTranslationServicesConfig().getLangDetectConfig().getSupported().contains(lang.toLowerCase());
  }
  
}
