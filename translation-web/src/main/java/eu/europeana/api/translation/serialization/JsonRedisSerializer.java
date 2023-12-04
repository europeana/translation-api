package eu.europeana.api.translation.serialization;

import java.io.IOException;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.definitions.model.CachedTranslation;

/**
 * Json Serializer for objects managed with redis cache  
 * @author GordeaS
 *
 */
public class JsonRedisSerializer implements RedisSerializer<Object> {
  private final ObjectMapper om;

  /**
   * Default constructor initializing the json object mapper
   */
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
  public CachedTranslation deserialize(byte[] bytes) throws SerializationException {
    if(bytes == null){
      return null;
    }			
    try {
      return om.readValue(bytes, CachedTranslation.class);
    } catch (IOException e) {
      throw new SerializationException(e.getMessage(), e);
    }
  }
	      
}
