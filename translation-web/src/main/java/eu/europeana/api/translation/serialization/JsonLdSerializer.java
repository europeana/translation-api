package eu.europeana.api.translation.serialization;

import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.BEAN_JSON_MAPPER;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonLdSerializer {

  private final ObjectMapper mapper;

  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  @Autowired
  public JsonLdSerializer(@Qualifier(BEAN_JSON_MAPPER) ObjectMapper objectMapper) {
    mapper = objectMapper.copy();
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
    mapper.setDateFormat(df);
  }

  public String serializeObject(Object obj) throws IOException {
    return mapper.writeValueAsString(obj);
  }
  
}
