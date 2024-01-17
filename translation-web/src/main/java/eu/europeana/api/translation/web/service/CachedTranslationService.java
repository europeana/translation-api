package eu.europeana.api.translation.web.service;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.definitions.model.TranslationObj;
import eu.europeana.api.translation.service.AbstractTranslationService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.service.exception.TranslationException;

public class CachedTranslationService extends AbstractTranslationService {
  private final RedisCacheService redisCacheService;
  private final TranslationService translationService;
  
  /*
   * The pangeanic translation service is used to detect the source languages of the input texts,
   * before the lookup to the cache is made.
   */
  public CachedTranslationService(RedisCacheService redisCacheService, @NotNull TranslationService translationService) {
    super();
    this.redisCacheService = redisCacheService;
    this.translationService = translationService;
  }

  @Override
  public String getServiceId() {
    return translationService.getServiceId();
  }
  
  @Override
  public void setServiceId(String serviceId) {
    
  }
  
  @Override
  public boolean isSupported(String srcLang, String trgLang) {
    return true;
  }
  
  @Override
  public void translate(List<TranslationObj> translationStrings) throws TranslationException {
    //fill the non translatable texts, e.g. empty Strings
    processNonTranslatable(translationStrings);
    
    if(isCachingEnabled()) {
      redisCacheService.fillWithCachedTranslations(translationStrings);
    }

    List<TranslationObj> toTranslate = translationStrings.stream().filter(
        t -> t.getTranslation() == null).toList();
    
    if(toTranslate.isEmpty()) {
      //all entries retrieved from cache, processing complete
      return;
    }
    
    translationService.translate(toTranslate);
    
    if(isCachingEnabled()) {
      //save result in the redis cache
      redisCacheService.store(toTranslate);  
    }
  }

  void processNonTranslatable(List<TranslationObj> translationStrings) {
    for (TranslationObj translationString : translationStrings) {
      if(StringUtils.isEmpty(translationString.getText())){
        translationString.setTranslation("");
      }
    }
  }

  @Override
  public void close() {
  }
  
  @Override
  public String getExternalServiceEndPoint() {
    return null;
  }

  private boolean isCachingEnabled() {
    return getRedisCacheService() != null;
  }

  public RedisCacheService getRedisCacheService() {
    return redisCacheService;
  }
}
