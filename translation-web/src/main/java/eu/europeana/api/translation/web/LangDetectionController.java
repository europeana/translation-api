package eu.europeana.api.translation.web;

import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.LANG;
import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.TEXT;
import static eu.europeana.api.translation.schemavalidation.JsonSchemaLocation.JSON_SCHEMA;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_INVALID_PARAM_VALUE;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_MANDATORY_PARAM_EMPTY;

import eu.europeana.api.translation.schemavalidation.ValidJson;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.web.exception.ParamValidationException;
import eu.europeana.api.translation.web.service.LangDetectionWebService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Language Detection endoints", description = "Perform language detection")
public class LangDetectionController extends BaseRest {

  private final LangDetectionWebService langDetectionService;

  public LangDetectionController(@Autowired LangDetectionWebService langDetectionService) {
    this.langDetectionService = langDetectionService;
  }

  @Tag(description = "Language detection", name = "detectLang")
  @PostMapping(value = {"/detect"},
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> detectLang(@ValidJson(JSON_SCHEMA)  LangDetectRequest langDetectRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    //validateRequest(langDetectRequest);  - validation to be performed via Json schema validation annotation

    LangDetectResponse result = langDetectionService.detectLang(langDetectRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

  private void validateRequest(LangDetectRequest langDetectRequest)
      throws ParamValidationException {
    // validate mandatory params
    if (langDetectRequest.getText() == null || containsNullValues(langDetectRequest.getText())) {
      throw new ParamValidationException(null, ERROR_MANDATORY_PARAM_EMPTY,
          ERROR_MANDATORY_PARAM_EMPTY, new String[] {TEXT});
    }
    // validate language hint if provided
    if (langDetectRequest.getLang() != null
        && !langDetectionService.isLangDetectionSupported(langDetectRequest.getLang())) {
      throw new ParamValidationException(null, ERROR_INVALID_PARAM_VALUE, ERROR_INVALID_PARAM_VALUE,
          new String[] {LANG, langDetectRequest.getLang()});
    }
  }


}
