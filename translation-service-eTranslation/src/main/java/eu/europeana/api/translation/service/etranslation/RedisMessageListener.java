package eu.europeana.api.translation.service.etranslation;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class RedisMessageListener implements MessageListener {

    private static final Logger LOGGER = LogManager.getLogger(RedisMessageListener.class);
    private String message;
    
    @Override
    public synchronized void onMessage(Message message, byte[] pattern) {
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug("New message received from RedisMessageListener: {}", message);
      }
      this.message=new String(message.getBody(), StandardCharsets.UTF_8);
      
      /* 
       * the received message is treated as a json object and we need some adjustments for the escaped characters
       * (this only applies if we get the translated text from the translated-text field in the eTransl callback,
       * which happens if we send the text to be translated in the textToTranslate request param)
       */
      //remove double quotes at the beginning and at the end of the response, from some reason they are duplicated
//      String messageRemDuplQuotes = messageBody.replaceAll("^\"|\"$", "");
      //replace a double backslash with a single backslash
//      this.message = messageRemDuplQuotes.replace("\\n", "\n");

      //notify all threads waiting on this object
      notifyAll();
    }

    public String getMessage() {
      return message;
    }

}