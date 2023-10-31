package eu.europeana.api.translation.web;

import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_INVALID_PARAM_VALUE;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_MANDATORY_PARAM_EMPTY;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons.error.EuropeanaI18nApiException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.web.exception.ParamValidationException;
import eu.europeana.api.translation.web.service.RedisCacheService;
import eu.europeana.api.translation.web.service.TranslationWebService;
import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@Tag(name = "Translation endpoint", description = "Perform text translation")
public class TranslationController extends BaseRest {

  private final TranslationWebService translationService;
  private final RedisCacheService redisCacheService;

  @Autowired
  public TranslationController(TranslationWebService translationService, RedisCacheService redisCacheService) {
    this.translationService = translationService;
    this.redisCacheService = redisCacheService;
  }

  @Tag(description = "Translation", name = "translate")
  @PostMapping(value = {"/translate"},
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> translate(@RequestBody TranslationRequest translRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    validateRequest(translRequest);
    
    TranslationResponse result = null;
    if(translRequest.isCaching()) {
      result = getCombinedCachedAndTranslatedResults(translRequest);
    }
    else {
      result = translationService.translate(translRequest);
    }

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

  private void validateRequest(TranslationRequest translationRequest) throws ParamValidationException {
    // validate mandatory params
    if (translationRequest.getText() == null) {
      throw new ParamValidationException(null, null, ERROR_MANDATORY_PARAM_EMPTY, new String[] {TranslationAppConstants.TEXT});
    }

    if (StringUtils.isEmpty(translationRequest.getTarget())) {
      throw new ParamValidationException(null, null, ERROR_MANDATORY_PARAM_EMPTY, new String[] {TranslationAppConstants.TARGET_LANG});
    }
    
    //validate language pair
    final LanguagePair languagePair = new LanguagePair(translationRequest.getSource(), translationRequest.getTarget());
    if(!translationService.isTranslationSupported(languagePair)) {
      throw new ParamValidationException(null, null, ERROR_INVALID_PARAM_VALUE, new String[] {LanguagePair.generateKey(TranslationAppConstants.SOURCE_LANG, TranslationAppConstants.TARGET_LANG) , languagePair.toString()});
    }
  }
  
  private TranslationResponse getCombinedCachedAndTranslatedResults(TranslationRequest translRequest) throws EuropeanaI18nApiException {
    TranslationResponse result=null;
    List<String> redisResp = redisCacheService.getRedisCache(translRequest.getSource(), translRequest.getTarget(), translRequest.getText());
    if(Collections.frequency(redisResp, null)>0) {
      TranslationRequest newTranslReq = new TranslationRequest(translRequest);
      List<String> newText = new ArrayList<String>();
      for(int i=0;i<redisResp.size();i++) {
        if(redisResp.get(i)==null) {
          newText.add(translRequest.getText().get(i));
        }
      }
      newTranslReq.setText(newText);
      result = translationService.translate(newTranslReq);
      
      //save the translations to the cache
      redisCacheService.saveRedisCache(newTranslReq.getSource(), newTranslReq.getTarget(), newTranslReq.getText(), result.getTranslations());
      
      //aggregate the redis and translation responses
      List<String> finalText=new ArrayList<String>(redisResp);
      int counterTranslated = 0;
      for(int i=0;i<finalText.size();i++) {
        if(finalText.get(i)==null) {
          finalText.set(i, result.getTranslations().get(counterTranslated));
          counterTranslated++;
        }
      }
      result.setService(null);
      result.setTranslations(finalText);       
    }
    else {
      result=new TranslationResponse();
      result.setLang(translRequest.getTarget());
      result.setTranslations(redisResp);
    }

    return result;
  }
  
}
