package eu.europeana.api.translation.web;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import eu.europeana.api.commons.web.http.HttpHeaders;

/**
 * Created by luthien on 2019-08-13.
 */
@RestController
public class TranslationErrorController extends AbstractErrorController {

    public TranslationErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }


    @GetMapping(value = "/error", produces = {HttpHeaders.CONTENT_TYPE_JSON_UTF8, HttpHeaders.CONTENT_TYPE_JSONLD})
    @ResponseBody
    public Map<String, Object> error(final HttpServletRequest request) {
        return this.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    }
    
}
