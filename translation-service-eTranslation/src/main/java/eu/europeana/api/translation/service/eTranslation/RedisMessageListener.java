package eu.europeana.api.translation.service.eTranslation;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class RedisMessageListener implements MessageListener {

    private static final Logger logger = LogManager.getLogger(RedisMessageListener.class);
    private String message;
    
    @Override
    public synchronized void onMessage(Message message, byte[] pattern) {
      logger.info("New message received from RedisMessageListener: {}", message);
      this.message=new String(message.getBody(), StandardCharsets.UTF_8);
      //notify all threads waiting on this object
      notifyAll();
    }

    public String getMessage() {
      return message;
    }

}