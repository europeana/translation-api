package eu.europeana.api.translation.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.definitions.model.CachedTranslation;
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
   * @param translationObjs the list of objects for which the translations will be searched in the cache 
   */
  public void fillWithCachedTranslations(List<TranslationObj> translationObjs) {
    // generate keys and list of cacheable translations
    List<String> cacheKeys = new ArrayList<>();
    List<TranslationObj> cacheableTranslations = new ArrayList<>();
    String redisKey;
    for (TranslationObj translationObj : translationObjs) {
      if (translationObj.getTranslation() == null && isCacheable(translationObj)) {
        // generate redis key and add translation to the list of cacheable objects
        redisKey = generateRedisKey(translationObj.getText(), translationObj.getSourceLang(),
            translationObj.getTargetLang());
        cacheKeys.add(redisKey);
        cacheableTranslations.add(translationObj);
      }
    }

    // get cached translations
    List<CachedTranslation> redisResponse = redisTemplate.opsForValue().multiGet(cacheKeys);
    if (redisResponse == null || redisResponse.size() != cacheableTranslations.size()) {
      // ensure that the response size corresponds to request size
      // this should not happen, but better use defensive programming
      logger.warn("Redis response size {} doesn't match the request size{}, for keys: {}",
          redisResponse.size(), cacheableTranslations.size(), cacheKeys);
      return;
    }

    // Accumulate cached translations to translation objects
    for (int i = 0; i < redisResponse.size(); i++) {
      updateFromCachedTranslation(cacheableTranslations.get(i), redisResponse.get(i),
          cacheKeys.get(i));
    }
  }

  /**
   * Update with translation object with the values of the cached translation corresponding to the given cache key
   * @param translationObj the object to cumulate the cached translation 
   * @param cachedTranslation translation found in the cache
   * @param cacheKey the redis key of the cached translations
   */
  private void updateFromCachedTranslation(TranslationObj translationObj,
      CachedTranslation cachedTranslation, final String cacheKey) {
    if (cachedTranslation != null && cachedTranslation.getTranslation() != null) {
      // update set key and translation, the the reference is to the same object as in the input
      // list
      translationObj.setTranslation(cachedTranslation.getTranslation());
      translationObj.setIsCached(true);
      translationObj.setCacheKey(cacheKey);
    }
  }

  private boolean isCacheable(TranslationObj translationObj) {
    return translationObj.getSourceLang() != null
        && StringUtils.isNotEmpty(translationObj.getText());
  }

  /**
   * Method to store translations into the cache. Only objects that are not marked as existing in the cache and fullfiling the {@link #isCacheable(TranslationObj)} criteria will be written into the cache
   * @param translationObjs the translations to be written into the cache 
   */
  public void store(List<TranslationObj> translationObjs) {
    Map<String, CachedTranslation> valueMap = new HashMap<>();
    String key;
    for (TranslationObj translObj : translationObjs) {
      if (isCacheable(translObj) && !translObj.getIsCached()) {
        // String key = translObj.getCacheKey();
        key = generateRedisKey(translObj.getText(), translObj.getSourceLang(),
            translObj.getTargetLang());
        translObj.setCacheKey(key);
        valueMap.put(key, toCachedTranslation(translObj));
      }
    }
    
    //write values to redis cache
    if(!valueMap.isEmpty()){
      redisTemplate.opsForValue().multiSet(valueMap);
    }
  }

  private CachedTranslation toCachedTranslation(TranslationObj translObj) {
    CachedTranslation cachedTranslation;
    cachedTranslation = new CachedTranslation();
    cachedTranslation.setOriginal(translObj.getText());
    cachedTranslation.setTranslation(translObj.getTranslation());
    return cachedTranslation;
  }

  
  /**
   * evict redis cache
   */
  public void deleteAll() {
    RedisConnectionFactory connFact=redisTemplate.getConnectionFactory();
    if(connFact!=null) {
      connFact.getConnection().flushAll();
    }
  }

  /**
   * generate redis keys
   * 
   * @param inputText the original text
   * @param sourceLang language of the original text
   * @param targetLang language of the translation
   * @return generated redis key
   */
  public String generateRedisKey(String inputText, String sourceLang, String targetLang) {
    String key = inputText + sourceLang + targetLang;
    return String.valueOf(key.hashCode());
  }

}
