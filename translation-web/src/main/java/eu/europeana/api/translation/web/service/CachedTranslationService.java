package eu.europeana.api.translation.web.service;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

public class CachedTranslationService extends AbstractTranslationService {
  private final Logger logger = LogManager.getLogger(CachedTranslationService.class);
  private final RedisCacheService redisCacheService;
  private final TranslationService translationService;
  
  /*
   * The pangeanic translation service is used to detect the source languages of the input texts,
   * before the lookup to the cache is made.
   */
  public CachedTranslationService(RedisCacheService redisCacheService, @NotNull TranslationService translationService) {
    super();
    this.redisCacheService = redisCacheService;
    this.translationService = translationService;
  }

  @Override
  public String getServiceId() {
    return translationService.getServiceId();
  }
  
  @Override
  public void setServiceId(String serviceId) {
    
  }
  
  @Override
  public boolean isSupported(String srcLang, String trgLang) {
    return true;
  }
  
  @Override
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    //fill the non translatable texts, e.g. empty Strings
    processNonTranslatable(translationObjs);
    
     
    fillTranslationForSameLanguage(translationObjs);
    
    
    if(isCachingEnabled()) {
      redisCacheService.fillWithCachedTranslations(translationObjs);
    }
    
    List<TranslationObj> toTranslate = translationObjs.stream().filter(
        t -> t.getTranslation() == null).toList();

    if(!toTranslate.isEmpty()) {    
      translationService.translate(toTranslate);
      if(isCachingEnabled()) {
        //save result in the redis cache
        redisCacheService.store(toTranslate);  
      }
    }
    
    //logging the number of translated/cached lines and chars
    int numLinesCached=(int) translationObjs.stream().filter(el -> el.isRetrievedFromCache()).count();
    int numCharsCached=translationObjs.stream().filter(el -> el.isRetrievedFromCache()).map(el -> el.getTranslation().length()).reduce(0, Integer::sum);
    int numLinesTranslated=toTranslate.size();
    int numCharsTranslated=toTranslate.stream().map(el -> el.getText().length()).reduce(0, Integer::sum);
    if(logger.isInfoEnabled()) {
      logger.info("Tracking cache usage: numLinesCached={}, numCharsCached={}, numLinesTranslated={}, "
          + "numCharsTranslated={}", numLinesCached, numCharsCached, numLinesTranslated, numCharsTranslated);
    }

    
  }


  @Override
  public void close() {
  }
  
  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

  private boolean isCachingEnabled() {
    return getRedisCacheService() != null;
  }

  public RedisCacheService getRedisCacheService() {
    return redisCacheService;
  }
}
