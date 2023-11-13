package eu.europeana.api.translation.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import eu.europeana.api.translation.model.CachedTranslation;

public class RedisCacheService {

  private final RedisTemplate<String, CachedTranslation> redisTemplate;
  
  /**
   * Service for remote invocation of redis caching system
   * @param redisTemplate the template for communicating with redis system
   */
  public RedisCacheService(RedisTemplate<String, CachedTranslation> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }
  
  /**
   * Returns a list of Objects (strings) that exist in the cache. If no cache is found for the given key,
   * the corresponding element in the return list will be null.
   * @param sourceLang the language of the input texts
   * @param targetLang the language of the translations
   * @param texts original texts for which the translation is required
   * @return the translation of the input text in the targetLanguage
   */
  public List<String> getCachedTranslations(String sourceLang, String targetLang, List<String> texts) {
    List<String> keys = new ArrayList<>();
    for(String text : texts){ 
      keys.add(generateRedisKey(text, sourceLang, targetLang));
    }
            
    List<CachedTranslation> redisResponse = redisTemplate.opsForValue().multiGet(keys);
    List<String> resp = new ArrayList<>();
    for(CachedTranslation respElem : redisResponse) {
      if(respElem!=null) {
        resp.add(respElem.getTranslation());
      }
      else {
        resp.add(null);
      }
    }
    return resp;
  }
  
  /**
   * This method saves the provided translations corresponding to the input texts into the redis cache 
   * @param sourceLang language of the inputText
   * @param targetLang the language of the translation
   * @param inputText the original text that was translated
   * @param translations the translations of the input text in the targetLang
   */
  public void saveRedisCache(String sourceLang, String targetLang, List<String> inputText, List<String> translations) {
    Map<String, CachedTranslation> valueMap = new HashMap<>(inputText.size());
    for(int i=0;i<inputText.size();i++) {
      String key = generateRedisKey(inputText.get(i), sourceLang, targetLang);
      CachedTranslation value = new CachedTranslation();
      value.setOriginal(inputText.get(i));
      value.setTranslation(translations.get(i));
      valueMap.put(key, value);
    }
   
    redisTemplate.opsForValue().multiSet(valueMap);
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
   * @param inputText the original text
   * @param sourceLang language of the original text
   * @param targetLang language of the translation
   * @return generated redis key 
   */
  private String generateRedisKey(String inputText, String sourceLang, String targetLang) {
    String key=inputText + sourceLang + targetLang;
    return String.valueOf(key.hashCode());
  }

}
