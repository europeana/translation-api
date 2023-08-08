package eu.europeana.api.translation.web.exception;

import javax.annotation.Resource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import eu.europeana.api.commons.config.i18n.I18nService;
import eu.europeana.api.commons.web.controller.exception.AbstractExceptionHandlingController;
import eu.europeana.api.translation.config.TranslationBeans;

@ControllerAdvice
public class GlobalExceptionHandler extends AbstractExceptionHandlingController {

	@Resource(name=TranslationBeans.BEAN_I18N_SERVICE)
	I18nService i18nService;

	protected I18nService getI18nService() {
		return i18nService;
	}

}
