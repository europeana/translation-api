package eu.europeana.api.translation.web.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import eu.europeana.api.commons.config.i18n.I18nService;
import eu.europeana.api.commons.web.exception.EuropeanaGlobalExceptionHandler;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.web.service.RequestPathMethodService;

@ControllerAdvice
@ConditionalOnWebApplication
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {

  I18nService i18nService;

  /**
   * Constructor for the initialization of the Exception handler
   * @param requestPathMethodService builtin service for path method mapping
   * @param i18nService the internationalization service
   */
  @Autowired
  public GlobalExceptionHandler(RequestPathMethodService requestPathMethodService,
      @Qualifier(BeanNames.BEAN_I18N_SERVICE) I18nService i18nService) {
    this.requestPathMethodService = requestPathMethodService;
    this.i18nService = i18nService;
  }

  @Override
  public I18nService getI18nService() {
    return i18nService;
  }

}
