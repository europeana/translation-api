package eu.europeana.api.translation.web;

import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_INVALID_PARAM_VALUE;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_MANDATORY_PARAM_EMPTY;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import eu.europeana.api.translation.web.exception.ParamValidationException;
import eu.europeana.api.translation.web.service.TranslationWebService;
import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@Tag(name = "Translation endpoint", description = "Perform text translation")
public class TranslationController extends BaseRest {

  private final TranslationWebService translationService;

  @Autowired
  public TranslationController(TranslationWebService translationService) {
    this.translationService = translationService;
  }

  @Tag(description = "Translation", name = "translate")
  @PostMapping(value = {"/translate"},
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> translate(@RequestBody TranslationRequest translRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    validateRequest(translRequest);
        
    TranslationResponse result = translationService.translate(translRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

  private void validateRequest(TranslationRequest translationRequest) throws ParamValidationException {
    // validate mandatory params
    if (translationRequest.getText() == null || containsNullValues(translationRequest.getText())) {
      throw new ParamValidationException(null, ERROR_MANDATORY_PARAM_EMPTY, ERROR_MANDATORY_PARAM_EMPTY, new String[] {TranslationAppConstants.TEXT});
    }

    if (StringUtils.isEmpty(translationRequest.getTarget())) {
      throw new ParamValidationException(null, ERROR_MANDATORY_PARAM_EMPTY, ERROR_MANDATORY_PARAM_EMPTY, new String[] {TranslationAppConstants.TARGET_LANG});
    }
    
    //validate language pair
    final LanguagePair languagePair = new LanguagePair(translationRequest.getSource(), translationRequest.getTarget());
    if(!translationService.isTranslationSupported(languagePair)) {
      throw new ParamValidationException(null, ERROR_MANDATORY_PARAM_EMPTY, ERROR_INVALID_PARAM_VALUE, new String[] {LanguagePair.generateKey(TranslationAppConstants.SOURCE_LANG, TranslationAppConstants.TARGET_LANG) , languagePair.toString()});
    }
  }
  
  
}
