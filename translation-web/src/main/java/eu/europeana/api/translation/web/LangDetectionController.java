package eu.europeana.api.translation.web;

import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.LANG;
import static eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants.TEXT;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons_sb3.definitions.http.HttpHeaders;
import eu.europeana.api.commons_sb3.definitions.oauth.Operations;
import eu.europeana.api.commons_sb3.error.EuropeanaI18nApiException;
import eu.europeana.api.commons_sb3.error.exceptions.InvalidParamException;
import eu.europeana.api.commons_sb3.error.exceptions.MissingParamException;
import eu.europeana.api.translation.definitions.model.LangDetectRequest;
import eu.europeana.api.translation.definitions.model.LangDetectResponse;
import eu.europeana.api.translation.web.service.LangDetectionWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "Language Detection endoints", description = "Perform language detection")
public class LangDetectionController extends BaseRest {

  private final LangDetectionWebService langDetectionService;

  public LangDetectionController(@Autowired LangDetectionWebService langDetectionService) {
    this.langDetectionService = langDetectionService;
  }

  @Operation(summary = "Language detection")
  @PostMapping(value = {"/detect"},
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> detectLang(@RequestBody LangDetectRequest langDetectRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    validateRequest(langDetectRequest);

    LangDetectResponse result = langDetectionService.detectLang(langDetectRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

  private void validateRequest(LangDetectRequest langDetectRequest)
      throws EuropeanaI18nApiException {
    // validate mandatory params
    if (langDetectRequest.getText() == null || containsNullValues(langDetectRequest.getText())) {
      throw new MissingParamException(List.of(TEXT));
    }
    // validate language hint if provided
    if (langDetectRequest.getLang() != null
        && !langDetectionService.isLangDetectionSupported(langDetectRequest.getLang())) {
      throw new InvalidParamException(List.of(LANG, "one of supported languages by detection service" , langDetectRequest.getLang()));
    }
  }


}
