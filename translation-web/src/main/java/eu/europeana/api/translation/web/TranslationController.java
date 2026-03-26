package eu.europeana.api.translation.web;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
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
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.model.TranslationRequest;
import eu.europeana.api.translation.definitions.model.TranslationResponse;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.web.service.TranslationWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "Translation endpoint", description = "Perform text translation")
public class TranslationController extends BaseRest {

  private final TranslationWebService translationService;

  @Autowired
  public TranslationController(TranslationWebService translationService) {
    this.translationService = translationService;
  }

  @Operation(summary = "Text Translation")
  @PostMapping(value = {"/translate"},
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> translate(@RequestBody TranslationRequest translRequest,
      HttpServletRequest request) throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    validateRequest(translRequest);

    if (logger.isTraceEnabled()) {
      logger.trace("Translation request: {}", jsonLdSerializer.serializeObject(translRequest));
    }

    TranslationResponse result = translationService.translate(translRequest);

    String resultJson = serialize(result);

    return generateResponseEntity(request, resultJson);
  }

  private void validateRequest(TranslationRequest translationRequest)
      throws EuropeanaI18nApiException {
    // validate mandatory params
    if (translationRequest.getText() == null || containsNullValues(translationRequest.getText())) {
      throw new MissingParamException(List.of(TranslationAppConstants.TEXT));
    }

    if (StringUtils.isEmpty(translationRequest.getTarget())) {
      throw new MissingParamException(List.of(TranslationAppConstants.TARGET_LANG));
    }

    // validate language pair
    final LanguagePair languagePair =
        new LanguagePair(translationRequest.getSource(), translationRequest.getTarget());
    if (!translationService.isTranslationSupported(languagePair)) {
      throw new InvalidParamException(
          List.of(LanguagePair.generateKey(TranslationAppConstants.SOURCE_LANG,
              TranslationAppConstants.TARGET_LANG), languagePair.toString()));
    }
  }


}
