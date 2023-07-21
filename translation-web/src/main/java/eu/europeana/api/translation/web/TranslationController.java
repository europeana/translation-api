package eu.europeana.api.translation.web;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.exception.ParamValidationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.config.serialization.TranslationServicesConfiguration;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.web.service.TranslationServiceImpl;
import io.swagger.annotations.ApiOperation;

@RestController
@ConditionalOnWebApplication
public class TranslationController extends BaseRest {

  private final TranslationServiceImpl translationService;

  @Autowired
  public TranslationController(TranslationServiceImpl translationService) {
    this.translationService=translationService;
  }

  @ApiOperation(
      value = "Get a configuration info",
      nickname = "getInfo",
      response = java.lang.Void.class)
  @RequestMapping(
      value = {"/actuator/info"},
      method = RequestMethod.GET,
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> getInfo(HttpServletRequest request) throws ApplicationAuthenticationException, EuropeanaApiException {

    verifyReadAccess(request);
    
    TranslationServicesConfiguration translGlobalConfig = translationService.info();
    
    String result = serialize(translGlobalConfig);
    
    return generateResponseEntity(request, result);
  }

  @ApiOperation(
      value = "Language detection",
      nickname = "detectLang",
      response = java.lang.Void.class)
  @RequestMapping(
      value = {"/detect"},
      method = RequestMethod.POST,
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> detectLang(@RequestBody LangDetectRequest langDetectRequest, HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, ParamValidationException {

    verifyWriteAccess(Operations.CREATE, request);

    LangDetectResponse result = translationService.detectLang(langDetectRequest);
    
    String resultJson = serialize(result);
    
    return generateResponseEntity(request, resultJson);
  }

  @ApiOperation(
      value = "Translation",
      nickname = "translate",
      response = java.lang.Void.class)
  @RequestMapping(
      value = {"/translate"},
      method = RequestMethod.POST,
      produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> translate(@RequestBody TranslationRequest translRequest, HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, ParamValidationException {

    verifyWriteAccess(Operations.CREATE, request);

    TranslationResponse result = translationService.translate(translRequest);
    
    String resultJson = serialize(result);
    
    return generateResponseEntity(request, resultJson);
  }
  
}
