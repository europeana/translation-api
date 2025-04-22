package eu.europeana.api.translation.web.service;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europeana.api.translation.definitions.model.TranslationCachingStats;
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
  
  private TranslationCachingStats computeTranslationCachingStats(List<TranslationObj> allTranslObjs) { 
    int numLinesCached=0;
    int numCharsCached=0;
    int numLinesToBeTranslated=0;
    int numCharsToBeTranslated=0;
    for(TranslationObj translObj : allTranslObjs) {
      if(translObj.isRetrievedFromCache()) {
        numLinesCached += 1;
        numCharsCached += translObj.getTranslation().length();
      }
      if(translObj.getTranslation() == null) {
        //objects sent for translation
        numLinesToBeTranslated += 1;
        numCharsToBeTranslated += translObj.getText().length();
      }
    }
    
    TranslationCachingStats stats = new TranslationCachingStats();
    stats.setNumLinesCached(numLinesCached);
    stats.setNumCharsCached(numCharsCached);
    stats.setNumLinesToBeTranslated(numLinesToBeTranslated);
    stats.setNumCharsToBeTranslated(numCharsToBeTranslated);
    return stats;
  }
  
  @Override
  public void translate(List<TranslationObj> translationObjs) throws TranslationException {
    //fill the non translatable texts, e.g. empty Strings
    processNonTranslatable(translationObjs);   
    
    if(isCachingEnabled()) {
      redisCacheService.fillWithCachedTranslations(translationObjs);
    }
    
    //logging the number of translated/cached lines and chars
    if(logger.isInfoEnabled()) {
      TranslationCachingStats stats = computeTranslationCachingStats(translationObjs);
      logger.info("Tracking cache usage: numLinesCached={}, numCharsCached={}, numLinesToBeTranslated={}, "
          + "numCharsToBeTranslated={}", stats.getNumLinesCached(), stats.getNumCharsCached(), 
          stats.getNumLinesToBeTranslated(), stats.getNumCharsToBeTranslated());
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
