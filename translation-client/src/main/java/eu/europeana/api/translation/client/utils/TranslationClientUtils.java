package eu.europeana.api.translation.client.utils;

import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Function;

public class TranslationClientUtils {

    public static final String TRANSLATE_URL = "translate";
    public static final String LANG_DETECT_URL = "detect";

    private TranslationClientUtils() {

    }

    /**
     * Builds the url path based on the path string passed
     * @param path
     * @return
     */
    public static Function<UriBuilder, URI> buildUrl(String path) {
        return uriBuilder -> {
            UriBuilder builder =
                    uriBuilder
                            .path("/" + path) ;
            return builder.build();
        };
    }
}
