package eu.europeana.api.translation.web.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {

  private RedisTemplate<Integer, String> redisTemplate;

  @Autowired
  public RedisCacheService(RedisTemplate<Integer, String> redisTemplate) {
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
    Collection<Integer> keys = new ArrayList<>();
    texts.stream().forEach(text -> {
      String key = text + sourceLang + targetLang;
      keys.add(key.hashCode());
    });
            
    List<String> redisResp = redisTemplate.opsForValue().multiGet(keys);
    return redisResp;
  }
  
  public void saveRedisCache(String sourceLang, String targetLang, List<String> inputText, List<String> translations) {
    Map<Integer, String> valueMap = new HashMap<>();
    for(int i=0;i<inputText.size();i++) {
      String key = inputText.get(i) + sourceLang + targetLang;
      valueMap.put(key.hashCode(), translations.get(i));
    }
   
    redisTemplate.opsForValue().multiSet(valueMap);
  }
  
  public void deleteAll() {
    redisTemplate.getConnectionFactory().getConnection().flushAll();
  }

}
