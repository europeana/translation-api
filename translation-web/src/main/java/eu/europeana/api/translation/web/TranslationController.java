package eu.europeana.api.translation.web;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.info.GitProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.api.translation.config.InitServicesGlobalJsonConfig;
import eu.europeana.api.translation.model.InfoResponse;
import eu.europeana.api.translation.model.LangDetectRequest;
import eu.europeana.api.translation.model.LangDetectResponse;
import eu.europeana.api.translation.model.TranslationRequest;
import eu.europeana.api.translation.model.TranslationResponse;
import eu.europeana.api.translation.service.TranslationServiceImpl;
import io.swagger.annotations.ApiOperation;

@RestController
@ConditionalOnWebApplication
public class TranslationController extends BaseRest {

  private final TranslationServiceImpl translationService;
  private GitProperties gitProperties;
  private InitServicesGlobalJsonConfig initGlobalJsonConfig;

  @Autowired
  public TranslationController(TranslationServiceImpl translationService, GitProperties gitProperties, InitServicesGlobalJsonConfig initGlobalJsonConfig) {
    this.translationService=translationService;
    this.gitProperties=gitProperties;
    this.initGlobalJsonConfig=initGlobalJsonConfig;
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
    InfoResponse infoResp = new InfoResponse();
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode build = mapper.createObjectNode();
    build.put("branch", gitProperties.get("branch"));
    build.put("number", gitProperties.get("commit.id.abbrev"));
    build.put("date", gitProperties.get("build.time"));
    infoResp.setBuild(build);
    ObjectNode app = mapper.createObjectNode();
    app.put("name", translationBuildInfo.get("project.name"));
    app.put("version", translationBuildInfo.get("version"));
    app.put("description", translationBuildInfo.get("project.description"));
    infoResp.setApp(app);
    infoResp.setConfig(initGlobalJsonConfig.getAppGlobalJsonConfig());
    
    String result = serialize(infoResp);
    
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
      throws Exception {

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
      throws Exception {

    verifyWriteAccess(Operations.CREATE, request);

    TranslationResponse result = translationService.translate(translRequest);
    
    String resultJson = serialize(result);
    
    return generateResponseEntity(request, resultJson);
  }
  
}
