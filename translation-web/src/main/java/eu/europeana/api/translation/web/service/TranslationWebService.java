package eu.europeana.api.translation.web.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.translation.config.I18nConstants;
import eu.europeana.api.translation.config.TranslationServiceConfigProvider;
import eu.europeana.api.translation.config.services.TranslationLangPairCfg;
import eu.europeana.api.translation.config.services.TranslationMappingCfg;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

@Service
public class TranslationWebService {

  @Autowired
  private TranslationServiceConfigProvider translationServiceConfigProvider;

  public TranslationResponse translate(TranslationRequest translationRequest) throws Exception {
    LanguagePair languagePair =
        new LanguagePair(translationRequest.getSource(), translationRequest.getTarget());
    TranslationService translationService = selectTranslationService(translationRequest, languagePair);
    TranslationService fallback = null;
    if(translationRequest.getFallback() != null) {
      fallback = getTranslationService(translationRequest.getFallback(), languagePair);
    }
    
    List<String> translations = null;
    try {
      translations = invokeTranslation(translationService, translationRequest);  
    } catch (Exception ePrimary) {
      // call the fallback service in case of failed translation
      if (fallback == null) {
        throw ePrimary;
      }
      translations = invokeTranslation(fallback, translationRequest);
    }
    TranslationResponse result = new TranslationResponse();
    result.setTranslations(translations);
    result.setLang(translationRequest.getTarget());
    return result;
  }

  private List<String> invokeTranslation(TranslationService translationService,
      TranslationRequest translationRequest) throws TranslationException {
    List<String> translations;
    if(translationRequest.getDetect()) {
      translations =
          translationService.translate(translationRequest.getText(), translationRequest.getTarget());  
    } else {
      translations =
          translationService.translate(translationRequest.getText(), translationRequest.getTarget(), translationRequest.getSource());
    }
    return translations;
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
    final String defaultServiceId = translationServiceConfigProvider.getTranslationServicesConfig()
        .getTranslationConfig().getDefaultServiceId();

    return getTranslationService(defaultServiceId, languagePair);
  }

  private TranslationService selectFromLanguageMappings(LanguagePair languagePair)
      throws ParamValidationException {
    for (TranslationMappingCfg translMappingCfg : translationServiceConfigProvider
        .getTranslationServicesConfig().getTranslationConfig().getMappings()) {

      if (translMappingCfg.isSupported(languagePair)) {
        return getTranslationService(translMappingCfg.getServiceId(), languagePair);
      }
    }
    return null;
  }

  private TranslationService getTranslationService(final String serviceId,
      LanguagePair languagePair) throws ParamValidationException{
    return getTranslationService(serviceId, languagePair, false);
  }
  private TranslationService getTranslationService(final String serviceId,
      LanguagePair languagePair, boolean fallback) throws ParamValidationException {
    TranslationService result =
        translationServiceConfigProvider.getTranslationServices().get(serviceId);
    String param = fallback ? TranslationAppConstants.FALLBACK : TranslationAppConstants.SERVICE;
    if (result == null) {
      throw new ParamValidationException(null, I18nConstants.INVALID_SERVICE_PARAM,
          new String[] {param, serviceId});
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
    List<TranslationLangPairCfg> langPairCfgList = translationServiceConfigProvider
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
}
