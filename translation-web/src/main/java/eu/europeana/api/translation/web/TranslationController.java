package eu.europeana.api.translation.web;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.config.I18nConstants;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.service.TranslationServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Translation & Detection endoints", description = "Perform language detection and translation")
public class TranslationController extends BaseRest {

  private final TranslationServiceImpl translationService;

  @Autowired
  public TranslationController(TranslationServiceImpl translationService) {
    this.translationService = translationService;
  }

  @Tag(description = "Language detection", name = "detectLang")
  @PostMapping(value = {"/detect"}, produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> detectLang(@RequestBody LangDetectRequest langDetectRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    // validate mandatory params
    if (langDetectRequest.getText() == null) {
      throw new ParamValidationException(null, I18nConstants.EMPTY_PARAM_MANDATORY,
          new String[] {TranslationAppConstants.TEXT});
    }

    LangDetectResponse result = translationService.detectLang(langDetectRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

  @Tag(description = "Translation", name = "translate")
  @PostMapping(value = {"/translate"}, produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> translate(@RequestBody TranslationRequest translRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    // validate mandatory params
    if (translRequest.getText() == null) {
      throw new ParamValidationException(null, I18nConstants.EMPTY_PARAM_MANDATORY,
          new String[] {TranslationAppConstants.TEXT});
    }
    if (translRequest.getSource() == null) {
      throw new ParamValidationException(null, I18nConstants.EMPTY_PARAM_MANDATORY,
          new String[] {TranslationAppConstants.SOURCE_LANG});
    }
    if (translRequest.getTarget() == null) {
      throw new ParamValidationException(null, I18nConstants.EMPTY_PARAM_MANDATORY,
          new String[] {TranslationAppConstants.TARGET_LANG});
    }

    TranslationResponse result = translationService.translate(translRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

}
