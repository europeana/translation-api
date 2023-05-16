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
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.translation.definitions.model.LangDetectRequestJsonConfig;
import eu.europeana.api.translation.definitions.model.LangDetectResponseJsonConfig;
import eu.europeana.api.translation.definitions.model.TranslGlobalJsonConfig;
import eu.europeana.api.translation.definitions.model.TranslRequestJsonConfig;
import eu.europeana.api.translation.definitions.model.TranslResponseJsonConfig;
import eu.europeana.api.translation.web.service.TranslService;
import io.swagger.annotations.ApiOperation;

@RestController
@ConditionalOnWebApplication
public class TranslController extends BaseRest {

  private final TranslService translService;

  @Autowired
  public TranslController(TranslService translService) {
    this.translService=translService;
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

//    verifyReadAccess(request);
    
    TranslGlobalJsonConfig translGlobalConfig = translService.info();
    
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
  public ResponseEntity<String> detectLang(@RequestBody LangDetectRequestJsonConfig langDetectRequest, HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {

//    verifyWriteAccess(Operations.CREATE, request);

    LangDetectResponseJsonConfig result = translService.detectLang(langDetectRequest);
    
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
  public ResponseEntity<String> translate(@RequestBody TranslRequestJsonConfig translRequest, HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {

//    verifyWriteAccess(Operations.CREATE, request);

    TranslResponseJsonConfig result = translService.translate(translRequest);
    
    String resultJson = serialize(result);
    
    return generateResponseEntity(request, resultJson);
  }
  
}
