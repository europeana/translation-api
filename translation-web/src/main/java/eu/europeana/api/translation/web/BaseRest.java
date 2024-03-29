package eu.europeana.api.translation.web;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.controller.BaseRestController;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.translation.config.TranslationConfig;
import eu.europeana.api.translation.serialization.JsonLdSerializer;
import eu.europeana.api.translation.web.service.RequestPathMethodService;
import eu.europeana.api.translation.web.service.TranslationAuthorizationService;

public abstract class BaseRest extends BaseRestController {

  @Autowired private TranslationAuthorizationService translAuthorizationService;

  @Autowired protected BuildProperties translationBuildInfo;

  @Autowired protected JsonLdSerializer jsonLdSerializer;

  @Autowired private RequestPathMethodService requestMethodService;

  @Autowired protected TranslationConfig translationConfig;

  protected Logger logger = LogManager.getLogger(getClass());

  public BaseRest() {
    super();
  }

  protected TranslationAuthorizationService getAuthorizationService() {
    return translAuthorizationService;
  }

  protected String getApiVersion() {
    return translationBuildInfo.getVersion();
  }

  protected String serialize(Object result) throws EuropeanaApiException {
    String responseBody = null;
    try {
      responseBody = jsonLdSerializer.serializeObject(result);
    } catch (IOException e) {
      throw new EuropeanaApiException("Error serializing results.", e);
    }
    return responseBody;
  }

  protected ResponseEntity<String> generateResponseEntity(HttpServletRequest request, String result) 
      throws EuropeanaApiException {
    // HttpHeaders.ALLOW
    org.springframework.http.HttpHeaders headers = createAllowHeader(request);
    headers.add(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_TYPE_JSON_UTF8);

    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(result);
  }

  protected org.springframework.http.HttpHeaders createAllowHeader(HttpServletRequest request) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    String allowHeaderValue;

    Optional<String> methodsForRequestPattern =
        requestMethodService.getMethodsForRequestPattern(request);
    if (methodsForRequestPattern.isEmpty()) {
      logger.warn(
          "Could not find other matching methods for {}. Using current request method in Allow header",
          request.getRequestURL());
      allowHeaderValue = request.getMethod();
    } else {
      allowHeaderValue = methodsForRequestPattern.get();
    }

    headers.add(HttpHeaders.ALLOW, allowHeaderValue);
    return headers;
  }

  protected ResponseEntity<String> noContentResponse(HttpServletRequest request) {
    return ResponseEntity.noContent().headers(createAllowHeader(request)).build();
  }

  @Override
  public Authentication verifyWriteAccess(String operation, HttpServletRequest request)
      throws ApplicationAuthenticationException {
    if (translationConfig.isAuthWriteEnabled()) {
      return super.verifyWriteAccess(operation, request);
    }
    return null;
  }

  @Override
  public Authentication verifyReadAccess(HttpServletRequest request)
      throws ApplicationAuthenticationException {
    if (translationConfig.isAuthReadEnabled()) {
      return super.verifyReadAccess(request);
    }
    return null;
  }
  

  protected boolean containsNullValues(List<String> texts) {
    for (String text : texts) {
      if(text == null) {
        return true;
      }
    }
    return false;
  }

}
