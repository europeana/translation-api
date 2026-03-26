package eu.europeana.api.translation.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons_sb3.definitions.oauth.Operations;
import eu.europeana.api.commons_sb3.definitions.utils.LoggingUtils;
import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.commons_sb3.oauth2.BaseRestController;
import eu.europeana.api.translation.service.etranslation.ETranslationTranslationService;
import eu.europeana.api.translation.web.model.CachedTranslation;
import eu.europeana.api.translation.web.service.TranslationAuthorizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "ETranslation callback controller", description = "Receives the eTranslation response")
/**
 * Callback to fetch eTranslation response
 */
public class ETranslationCallbackController extends BaseRestController{

  private static final Logger LOGGER = LogManager.getLogger(ETranslationCallbackController.class);

  @Resource private TranslationAuthorizationService translAuthorizationService;
  RedisTemplate<String, CachedTranslation> redisTemplate;

  @Autowired
  public ETranslationCallbackController(RedisTemplate<String, CachedTranslation> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }
  
  protected TranslationAuthorizationService getAuthorizationService() {
    return translAuthorizationService;
  }

  @Tag(description = "ETranslation callback endpoint", name = "eTranslationCallback")
  @PostMapping(value = ETranslationTranslationService.PATH_CALLBACK)
  /**
   * callback endpoint for eTranslation - post request
   */
  public void eTranslationCallbackPost(
      @RequestParam(value = "target-language", required = false) String targetLanguage,
      @RequestParam(value = "translated-text", required = false) String translatedTextSnippet,
      @RequestParam(value = "request-id", required = true) String requestId,
      @RequestParam(value = "external-reference", required = true) String externalReference,
      @RequestBody(required = false) String body) {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "eTranslation callback has been received with the request-id: {}, and the"
              + " external-reference: {}",
          LoggingUtils.sanitizeUserInput(requestId),
          LoggingUtils.sanitizeUserInput(externalReference));
    }
    /*
     * in case we send a document for the translation, we get the output in the body, or otherwise,
     * if we send a text snippet in the text-to-translate field, we get the output in the translated-text parameter 
     * (although also extracted from the body)
     */
    String translations = (translatedTextSnippet == null) ? body : translatedTextSnippet ;
    if(externalReference!=null && translations!=null) {
      redisTemplate.convertAndSend(externalReference, translations);
    }
  }

  /**
   * This method is deprecated, it is used for manual simulations only, as the eTranslation send post callbacks
   * @throws Exception 
   * @deprecated for simulation purposes only
   */
  @Tag(description = "ETranslation callback endpoint", name = "eTranslationCallback")
  @GetMapping(value = ETranslationTranslationService.PATH_CALLBACK)
  @Deprecated(since = "begiging ...")
  public ResponseEntity<String> eTranslationCallbackGet(
      @RequestParam(value = "target-language", required = false) String targetLanguage,
      @RequestParam(value = "translated-text", required = false) String translatedTextSnippet,
      @RequestParam(value = "request-id", required = false) String requestId,
      @RequestParam(value = "external-reference", required = false) String externalReference,
      @RequestParam(value = "timeout", required = false) Integer timeout,
      HttpServletRequest request) throws EuropeanaApiException {
    
    verifyWriteAccess(Operations.CREATE, request);
    
    if (timeout != null && timeout > 0) {
      // for simulation purposes, wait for $timeout seconds
      final long SECONDS_MILIS = 1000;
      try {
        Thread.sleep(timeout * SECONDS_MILIS);
      } catch (InterruptedException e) {
        /* Clean up whatever needs to be handled before interrupting  */
        Thread.currentThread().interrupt();
        throw new EuropeanaApiException("Cannot sleep thread!", e);
      }
      return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "eTranslation callback has been received with the request-id: {}, and the"
              + " external-reference: {}",
          LoggingUtils.sanitizeUserInput("" + requestId),
          LoggingUtils.sanitizeUserInput("" + externalReference));
    }
    if (externalReference != null && translatedTextSnippet != null) {
      redisTemplate.convertAndSend(externalReference, translatedTextSnippet);
    }

    return ResponseEntity.status(HttpStatus.ACCEPTED).build();

  }

  @Tag(description = "ETranslation error callback endpoint", name = "eTranslationErrorCallback")
  @PostMapping(value = ETranslationTranslationService.PATH_ERROR_CALLBACK)
  /**
   * callback endpoint for eTranslation errors - post request
   */
  public void eTranslationErrorCallbackPost(
      @RequestParam(value = "error-code", required = false) String errorCode,
      @RequestParam(value = "error-message", required = false) String errorMessage,
      @RequestParam(value = "target-languages", required = false) String targetLanguages,
      @RequestParam(value = "request-id", required = false) String requestId,
      @RequestParam(value = "external-reference", required = false) String externalReference,
      @RequestBody(required = false) String body) {
    handleErroCallback(errorCode, errorMessage, requestId, externalReference);
  }

  private void handleErroCallback(String errorCode, String errorMessage, String requestId,
      String externalReference) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "eTranslation error callback has been received with the following parameters: error-code: {},"
              + "error-message: {}, request-id: {}, external-reference: {}",
          LoggingUtils.sanitizeUserInput(errorCode), LoggingUtils.sanitizeUserInput(errorMessage),
          LoggingUtils.sanitizeUserInput(requestId),
          LoggingUtils.sanitizeUserInput(externalReference));
    }
    if (externalReference != null) {
      redisTemplate.convertAndSend(externalReference,
          String.format("%s: error-code=%s, error-message=%s",
              ETranslationTranslationService.ERROR_CALLBACK_MARKUP, errorCode,
              errorMessage));
    }
  }

  @Tag(description = "ETranslation error callback endpoint", name = "eTranslationErrorCallback")
  @GetMapping(value = ETranslationTranslationService.PATH_ERROR_CALLBACK)
  /**
   * callback endpoint for eTranslation - get request
   */
  public void eTranslationErrorCallbackGet(
      @RequestParam(value = "error-code", required = false) String errorCode,
      @RequestParam(value = "error-message", required = false) String errorMessage,
      @RequestParam(value = "target-languages", required = false) String targetLanguages,
      @RequestParam(value = "request-id", required = false) String requestId,
      @RequestParam(value = "external-reference", required = false) String externalReference,
      @RequestBody(required = false) String body) {
    handleErroCallback(errorCode, errorMessage, requestId, externalReference);
  }

}
