package eu.europeana.api.translation.serialization;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.model.RedisCacheTranslation;

public class JsonRedisSerializer implements RedisSerializer<Object> {
  private final ObjectMapper om;

  public JsonRedisSerializer() {
    this.om = new ObjectMapper();
  }

  @Override
  public byte[] serialize(Object t) throws SerializationException {
    try {
      return om.writeValueAsBytes(t);
    } catch (JsonProcessingException e) {
      throw new SerializationException(e.getMessage(), e);
    }
  }

  @Override
  public RedisCacheTranslation deserialize(byte[] bytes) throws SerializationException {
    if(bytes == null){
      return null;
    }			
    try {
      return om.readValue(bytes, RedisCacheTranslation.class);
    } catch (Exception e) {
      throw new SerializationException(e.getMessage(), e);
    }
  }
	      
}
