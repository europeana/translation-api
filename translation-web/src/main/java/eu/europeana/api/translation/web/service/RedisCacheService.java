package eu.europeana.api.translation.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import eu.europeana.api.translation.definitions.model.TranslationObj;
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
  
  public void getCachedTranslations(List<TranslationObj> translationObjs) {
    List<Integer> indexesHavingSourceLangNotHavingTranslation = IntStream.range(0, translationObjs.size())
        .filter(i -> translationObjs.get(i).getSourceLang()!=null && translationObjs.get(i).getTranslation()==null)
        .boxed()
        .collect(Collectors.toList());
    if(indexesHavingSourceLangNotHavingTranslation.isEmpty()) {
      return;
    }
    
    String targetLang = translationObjs.get(indexesHavingSourceLangNotHavingTranslation.get(0)).getTargetLang();
    List<String> texts = indexesHavingSourceLangNotHavingTranslation.stream()
        .map(i -> translationObjs.get(i).getText())
        .collect(Collectors.toList());

    List<String> keys = new ArrayList<>();
    for(int i=0;i<indexesHavingSourceLangNotHavingTranslation.size();i++){ 
      String redisKey = translationObjs.get(indexesHavingSourceLangNotHavingTranslation.get(i)).getCacheKey();
      if(redisKey==null) {
          redisKey=generateRedisKey(texts.get(i), translationObjs.get(indexesHavingSourceLangNotHavingTranslation.get(i)).getSourceLang(), targetLang);
          translationObjs.get(indexesHavingSourceLangNotHavingTranslation.get(i)).setCacheKey(redisKey);
      }
      keys.add(redisKey);
    }
            
    List<CachedTranslation> redisResponse = redisTemplate.opsForValue().multiGet(keys);
    for(int i=0;i<indexesHavingSourceLangNotHavingTranslation.size();i++) {
      if(redisResponse.get(i)!=null) {
        translationObjs.get(indexesHavingSourceLangNotHavingTranslation.get(i)).setTranslation(redisResponse.get(i).getTranslation());
        translationObjs.get(indexesHavingSourceLangNotHavingTranslation.get(i)).setIsCached(true);
      }
    }
  }
  
  public void saveRedisCache(List<TranslationObj> translationObjs) {
    Map<String, CachedTranslation> valueMap = new HashMap<>();
    for(TranslationObj translObj : translationObjs) {
      if(!translObj.getIsCached() && translObj.getSourceLang()!=null && translObj.getTranslation()!=null) {
        CachedTranslation value = new CachedTranslation();
        String key = translObj.getCacheKey();
        if(key==null) {
          key = generateRedisKey(translObj.getText(), translObj.getSourceLang(), translObj.getTargetLang());
          translObj.setCacheKey(key);
        }
        value.setOriginal(translObj.getText());
        value.setTranslation(translObj.getTranslation());
        valueMap.put(key, value);
      }
    }
   
    if(!valueMap.isEmpty()) {
      redisTemplate.opsForValue().multiSet(valueMap);
    }
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
