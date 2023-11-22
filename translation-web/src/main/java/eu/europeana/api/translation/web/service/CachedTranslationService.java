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
  private String serviceId;
  
  /*
   * The pangeanic translation service is used to detect the source languages of the input texts,
   * before the lookup to the cache is made.
   */
  public CachedTranslationService(RedisCacheService redisCacheService, TranslationService translationService) {
    super();
    this.redisCacheService = redisCacheService;
    this.translationService = translationService;
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
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    redisCacheService.getCachedTranslations(translationObjs);
    boolean anyCachedTransl = translationObjs.stream().filter(el -> el.getIsCached()).collect(Collectors.toList()).size()>0;
    //if there is any translation in the cache set the serviceId to null, because we do not know which service translated that
    if(anyCachedTransl) {
      setServiceId(null);
    }
    translationService.translate(translationObjs);
    redisCacheService.saveRedisCache(translationObjs);
  }

  @Override
  public void close() {
  }
  
  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

}
