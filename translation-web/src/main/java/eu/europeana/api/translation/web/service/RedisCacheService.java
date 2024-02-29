package eu.europeana.api.translation.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.util.UtilityMethods;
import eu.europeana.api.translation.web.model.CachedTranslation;
import io.micrometer.core.instrument.util.StringUtils;

public class RedisCacheService {

  private final RedisTemplate<String, CachedTranslation> redisTemplate;
  private final Logger logger = LogManager.getLogger(getClass());


  /**
   * Service for remote invocation of redis caching system
   * 
   * @param redisTemplate the template for communicating with redis system
   */
  public RedisCacheService(RedisTemplate<String, CachedTranslation> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Fills the translation texts and cache keys if the are available in redis cache
   * 
   * @param translationObjects the list of objects for which the translations will be searched in
   *        the cache
   */
  public void fillWithCachedTranslations(List<TranslationObj> translationObjects) {
    // generate keys and list of cacheable translations
    List<String> cacheKeys = new ArrayList<>();
    List<TranslationObj> cacheableTranslations = new ArrayList<>();
    String redisKey;
    for (TranslationObj translationObj : translationObjects) {
      if (translationObj.getTranslation() == null && isCacheable(translationObj) && !translationObj.isTranslated()) {
        // generate redis key and add translation to the list of cacheable objects
        redisKey = UtilityMethods.generateRedisKey(translationObj.getText(), translationObj.getSourceLang(),
            translationObj.getTargetLang(), false);
        cacheKeys.add(redisKey);
        cacheableTranslations.add(translationObj);
      }
    }

    if(cacheKeys.isEmpty()) {
      //no translations to be searched in the cache, no request to the caching service required
      return;
    }
      
    // get cached translations
    List<CachedTranslation> redisResponse = redisTemplate.opsForValue().multiGet(cacheKeys);
    if (redisResponse == null || redisResponse.size() != cacheableTranslations.size()) {
      // ensure that the response size corresponds to request size
      // this should not happen, but better use defensive programming
      int redisSize = redisResponse == null ? 0 : redisResponse.size();
      logger.warn("Redis response size {} doesn't match the request size{}, for keys: {}",
          redisSize, cacheableTranslations.size(), cacheKeys);
      return;
    }

    // Accumulate cached translations to translation objects
    for (int i = 0; i < redisResponse.size(); i++) {
      updateFromCachedTranslation(cacheableTranslations.get(i), redisResponse.get(i),
          cacheKeys.get(i));
    }
  }

  /**
   * Update with translation object with the values of the cached translation corresponding to the
   * given cache key
   * 
   * @param translationString the object to cumulate the cached translation
   * @param cachedTranslation translation found in the cache
   * @param cacheKey the redis key of the cached translations
   */
  private void updateFromCachedTranslation(TranslationObj translationString,
      CachedTranslation cachedTranslation, final String cacheKey) {
    if (cachedTranslation != null && cachedTranslation.getTranslation() != null) {
      // update set key and translation, the the reference is to the same object as in the input
      // list
      translationString.setTranslation(cachedTranslation.getTranslation());
      translationString.setRetrievedFromCache(true);
      translationString.setCacheKey(cacheKey);
    }
  }

  /**
   * verifies is the source language and text are available in the object This method is used both
   * for for verifying the cacheability for retrieval and for storage 
   * NOTE: currently we rely that
   * the calling methods are verifying the availability of the target language and original text
   * 
   * @param translationObj the translation object to verify if it should be cached
   * @param checkTranslationAvailable indicate if the availability of the translation needs to be
   *        checked (use true when storing and false )
   * @return true is source language and text are available, and source language is different from target language
   */
  private boolean isCacheable(TranslationObj translationObj) {
    return translationObj.getSourceLang() != null
        && !Objects.equals(translationObj.getTargetLang(), translationObj.getSourceLang())
        && StringUtils.isNotEmpty(translationObj.getText());
  }

  /**
   * This method indicates if the object has the target language and the translation available
   * 
   * @param translationString object to verify
   * @return true is both the target language and the translation are available
   */
  private boolean hasTranslation(TranslationObj translationString) {
    return translationString.getTargetLang() != null
        && StringUtils.isNotEmpty(translationString.getTranslation());
  }

  /**
   * Method to store translations into the cache. Only objects that are not marked as existing in
   * the cache and fullfiling the {@link #isCacheable(TranslationObj)} criteria will be written into
   * the cache
   * 
   * @param translationStrings the translations to be written into the cache
   */
  public void store(List<TranslationObj> translationStrings) {
    Map<String, CachedTranslation> valueMap = new HashMap<>();
    String key;
    for (TranslationObj translObj : translationStrings) {
      if (isCacheable(translObj) && hasTranslation(translObj) && !translObj.isRetrievedFromCache()) {
        // String key = translObj.getCacheKey();
        key = UtilityMethods.generateRedisKey(translObj.getText(), translObj.getSourceLang(),
            translObj.getTargetLang(), false);
        translObj.setCacheKey(key);
        valueMap.put(key, toCachedTranslation(translObj));
      }
    }

    // write values to redis cache
    if (!valueMap.isEmpty()) {
      redisTemplate.opsForValue().multiSet(valueMap);
    }
  }

  private CachedTranslation toCachedTranslation(TranslationObj translationObj) {
    CachedTranslation cachedTranslation;
    cachedTranslation = new CachedTranslation();
    cachedTranslation.setOriginal(translationObj.getText());
    cachedTranslation.setTranslation(translationObj.getTranslation());
    return cachedTranslation;
  }


  /**
   * evict redis cache
   */
  public void deleteAll() {
    RedisConnectionFactory connFact = redisTemplate.getConnectionFactory();
    if (connFact != null) {
      connFact.getConnection().flushAll();
    }
  }

}
