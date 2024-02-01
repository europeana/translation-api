package eu.europeana.api.translation.web.utils;

import eu.europeana.api.translation.definitions.model.TranslationObj;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PreProcessorUtils {

    /**
     * Any value that has at least 2 unicode consecutive letters. The condition considered the
     * fact that there can be words with only 2 letters that retain sufficient meaning and are therefore reasonable to be translated,
     * especially when looking at languages other than English (see article - https://www.grammarly.com/blog/the-shortest-words-in-the-english-language/).
     */
    private static final String PATTERN = "\\p{IsAlphabetic}{2,}";
    private static final Pattern isAlphabetic = Pattern.compile(PATTERN);


    /**
     * Check if the text present is an eligible value.
     * Eligible Value : Any value that has at least 2 unicode consecutive letters.
     * If value is not eligible, set isTranslated as false, which means we will not translate that text/value
     * @param translationObjs
     * @return
     */
    public static void processForEligibleValues(List<TranslationObj> translationObjs) {
        for (TranslationObj obj : translationObjs) {
            if (!isAlphabetic.matcher(obj.getText()).find()) {
                obj.setIsTranslated(false);
            }
        }
    }

    public static List<String> filterEligibleValues(List<String> texts) {
        return texts.stream().filter(value -> isAlphabetic.matcher(value).find()).collect(Collectors.toList());
    }

}
