package eu.europeana.api.translation.web.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import eu.europeana.api.translation.model.RedisCacheTranslation;

public class RedisCacheService {

  private final RedisTemplate<String, RedisCacheTranslation> redisTemplate;
  
  
  public RedisCacheService(RedisTemplate<String, RedisCacheTranslation> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }
  
  /**
   * Returns a list of Objects (strings) that exist in the cache. If no cache is found for the given key,
   * the corresponding element in the return list will be null.
   * @param sourceLang
   * @param targetLang
   * @param texts
   * @return
   */
  public List<String> getRedisCache(String sourceLang, String targetLang, List<String> texts) {
    Collection<String> keys = new ArrayList<>();
    texts.stream().forEach(text -> {
      keys.add(generateRedisKey(text, sourceLang, targetLang));
    });
            
    List<RedisCacheTranslation> redisResponse = redisTemplate.opsForValue().multiGet(keys);
    List<String> resp = new ArrayList<String>();
    for(RedisCacheTranslation respElem : redisResponse) {
      if(respElem!=null) {
        resp.add(respElem.getTranslation());
      }
      else {
        resp.add(null);
      }
    }
    return resp;
  }
  
  public void saveRedisCache(String sourceLang, String targetLang, List<String> inputText, List<String> translations) {
    Map<String, RedisCacheTranslation> valueMap = new HashMap<>(inputText.size());
    for(int i=0;i<inputText.size();i++) {
      String key = generateRedisKey(inputText.get(i), sourceLang, targetLang);
      RedisCacheTranslation value = new RedisCacheTranslation();
      value.setOriginal(inputText.get(i));
      value.setTranslation(translations.get(i));
      valueMap.put(key, value);
    }
   
    redisTemplate.opsForValue().multiSet(valueMap);
  }
  
  public void deleteAll() {
    RedisConnectionFactory connFact=redisTemplate.getConnectionFactory();
    if(connFact!=null) {
      connFact.getConnection().flushAll();
    }
  }
  
  private String generateRedisKey(String inputText, String sourceLang, String targetLang) {
    String key=inputText + sourceLang + targetLang;
    return String.valueOf(key.hashCode());
  }

}
