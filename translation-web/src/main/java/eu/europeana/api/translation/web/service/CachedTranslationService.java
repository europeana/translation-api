package eu.europeana.api.translation.web.service;

import java.util.List;
import java.util.stream.Collectors;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

public class CachedTranslationService extends AbstractTranslationService {
  private RedisCacheService redisCacheService;
  private TranslationService translationService;
  private TranslationService translationServicePangeanic;
  private String serviceId;
  
  /*
   * The pangeanic translation service is used to detect the source languages of the input texts,
   * before the lookup to the cache is made.
   */
  public CachedTranslationService(RedisCacheService redisCacheService, TranslationService translationService, TranslationService translationServicePangeanic) {
    super();
    this.redisCacheService = redisCacheService;
    this.translationService = translationService;
    this.translationServicePangeanic = translationServicePangeanic;
    this.serviceId = translationService.getServiceId();
  }

  @Override
  public String getServiceId() {
    return serviceId;
  }
  
  @Override
  public void setServiceId(String serviceId) {
    this.serviceId=serviceId;
  }
  
  @Override
  public boolean isSupported(String srcLang, String trgLang) {
    return true;
  }
  
  @Override
  public List<String> translate(List<String> texts, String targetLanguage) throws TranslationException {
    return null;
  }
  
  @Override
  public void translate(List<TranslationObj> translationObjs, boolean detectLanguages) throws TranslationException {
    //first detect languages for the texts that do not have it using the pangeanic lang detect
    translationServicePangeanic.detectLanguages(translationObjs);
    //then check if the translations exist in cache
    redisCacheService.getCachedTranslations(translationObjs);
    boolean anyCachedTransl = translationObjs.stream().filter(el -> el.getIsCached()).collect(Collectors.toList()).size()>0;
    //if there is any translation in the cache set the serviceId to null, because we do not know which service translated that
    if(anyCachedTransl) {
      setServiceId(null);
    }
    //then translate those that do not exist
    translationService.translate(translationObjs, false);
    //and save those that do not exist in cache to the cache
    redisCacheService.saveRedisCache(translationObjs);
  }

  @Override
  public List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage) throws TranslationException {
    return null;
  }

  @Override
  public void close() {
  }
  
  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

  @Override
  public void detectLanguages(List<TranslationObj> translationObjs)
      throws TranslationException {
  }

}
