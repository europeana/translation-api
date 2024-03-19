package eu.europeana.api.translation.web;

import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.LANG;
import static eu.europeana.api.translation.web.I18nErrorMessageKeys.ERROR_INVALID_PARAM_VALUE;

import eu.europeana.api.commons.ValidJson;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.web.exception.ParamValidationException;
import eu.europeana.api.translation.web.service.LangDetectionWebService;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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
  public ResponseEntity<String> detectLang( @ValidJson(uri=TranslationSchemaLocation.JSON_SCHEMA_URI,nested = TranslationSchemaLocation.NESTED_SCHEMA_LANG_DETECT_REQUEST)  LangDetectRequest langDetectRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    validateRequest(langDetectRequest);

    LangDetectResponse result = langDetectionService.detectLang(langDetectRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }


  private void validateRequest(LangDetectRequest langDetectRequest)
      throws ParamValidationException {
    //   mandatory parameter validation will be performed using json schema

    // validate language hint if provided
    if (langDetectRequest.getLang() != null
        && !langDetectionService.isLangDetectionSupported(langDetectRequest.getLang())) {
      throw new ParamValidationException(null, ERROR_INVALID_PARAM_VALUE, ERROR_INVALID_PARAM_VALUE,
          new String[] {LANG, langDetectRequest.getLang()});
    }
  }


}
