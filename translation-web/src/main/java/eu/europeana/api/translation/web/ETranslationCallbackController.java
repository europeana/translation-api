package eu.europeana.api.translation.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.translation.service.eTranslation.ETranslationTranslationService;
import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@Tag(name = "ETranslation callback controller", description = "Receives the eTranslation response")
public class ETranslationCallbackController {

  private static final Logger logger = LogManager.getLogger(ETranslationCallbackController.class);
  
  private final ETranslationTranslationService eTranslationService;

  @Autowired
  public ETranslationCallbackController(ETranslationTranslationService eTranslationService) {
    this.eTranslationService = eTranslationService;
  }

  @Tag(description = "ETranslation callback endpoint", name = "eTranslationCallback")
  @RequestMapping(value = "/eTranslation/callback", method = {RequestMethod.POST}, produces = MediaType.TEXT_PLAIN_VALUE)
  public void eTranslationCallback(
      @RequestParam(value = "target-language", required = false) String targetLanguage,
      @RequestParam(value = "translated-text", required = false) String translatedTextSnippet,
      @RequestParam(value = "request-id", required = false) String requestId,
      @RequestParam(value = "external-reference", required = false) String externalReference) 
  {
    logger.info("eTranslation callback on translation api has been executed");
    eTranslationService.processCallback(targetLanguage,translatedTextSnippet,requestId,externalReference);
  }
 
  
}
