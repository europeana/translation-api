package eu.europeana.api.translation.web;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
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
  @PostMapping(value = {"/detect"}, produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> detectLang(@RequestBody LangDetectRequest langDetectRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    validateRequest(langDetectRequest);

    LangDetectResponse result = langDetectionService.detectLang(langDetectRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

  private void validateRequest(LangDetectRequest langDetectRequest)
      throws ParamValidationException {
    // validate mandatory params
    if (langDetectRequest.getText() == null) {
      throw new ParamValidationException(null, TranslationAppConstants.ERROR_MANDATORY_PARAM_EMPTY, TranslationAppConstants.ERROR_MANDATORY_PARAM_EMPTY, new String[] {TranslationAppConstants.TEXT});
    }
    //validate language hint if provided
    if(langDetectRequest.getLang() != null && !langDetectionService.isLangDetectionSupported(langDetectRequest.getLang())) {
      throw new ParamValidationException(null, TranslationAppConstants.ERROR_INVALID_PARAM_VALUE, TranslationAppConstants.ERROR_INVALID_PARAM_VALUE, new String[] {TranslationAppConstants.LANG, langDetectRequest.getLang()});
    }
  }

  
}
