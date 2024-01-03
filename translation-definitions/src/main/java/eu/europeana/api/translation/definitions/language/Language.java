package eu.europeana.api.translation.definitions.language;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.definitions.exceptions.InvalidLanguageException;

/**
 * Supported languages for filtering record data
 *
 * @author Srishti Singh
 * Created 2 March 2023
 */
public enum Language {

    EN, NL, FR, DE, ES, SV, IT, FI, DA, EL, CS, SK, SL, PT, HU, LT, PL, RO, BG, HR, LV, GA, MT, ET, NO, CA, RU;

    private static final Set<String> LANGUAGES = new HashSet<>(Stream.of(Language.values())
            .map(Enum::name)
            .collect(Collectors.toList()));

    private static final String SEPARATOR = ",";

    public static final String DEF = "def";
    public static final String NO_LINGUISTIC_CONTENT = "zxx";

    // pivot language - "en"
    public static final String PIVOT = Language.EN.name().toLowerCase(Locale.ROOT);

    /**
     * Validate if the provided string is a single 2-letter ISO-code language abbreviation
     * @param languageAbbrevation the string to check
     * @return Language that was found
     * @throws InvalidLanguageException if the string did not match any supported language
     */
    public static Language validateSingle(String languageAbbrevation) throws InvalidLanguageException {
        if (StringUtils.isBlank(languageAbbrevation)) {
            throw new InvalidLanguageException("Empty language value");
        }

        Language result;
        try {
            result = Language.valueOf(languageAbbrevation.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidLanguageException("Language value '" + languageAbbrevation + "' is not valid", e);
        }
        return result;
    }

    /**
     * Checks if the provided string consists of one or more 2 letter abbreviation of the supported languages.
     * @param languageAbbrevations String containing one or more two letter ISO-code abbreviation of a language, separated
     *                             by a comma (and optionally also a space)
     * @return a list of one or more found languages
     * @throws InvalidLanguageException if one of the values is incorrect
     */
    public static List<Language> validateMultiple(String languageAbbrevations) throws InvalidLanguageException {
        if (StringUtils.isBlank(languageAbbrevations)) {
            throw new InvalidLanguageException("Empty language value");
        }

        String[] languages = languageAbbrevations.split(SEPARATOR);
        List<Language> result = new ArrayList<>(languages.length);
        for (String language: languages) {
            result.add(validateSingle(language));
        }
        if (result.isEmpty()) {
            throw new InvalidLanguageException("Language value '" + languageAbbrevations + "' is not valid");
        }
        return result;
    }

    public static Language getLanguage(String lang) {
        return Language.valueOf(stripLangStringIfRegionPresent(lang).toUpperCase(Locale.ROOT));
    }

    /**
     * Check if a particular string is one of the supported languages
     * @param lang 2 letter ISO-code abbrevation of a language
     * @return true if we support it, otherwise false
     */
    public static boolean isSupported(String lang) {
        return LANGUAGES.contains(stripLangStringIfRegionPresent(lang).toUpperCase(Locale.ROOT));
    }

    /**
     * Check if the provided language code indicates no linguistic content
     * (see also https://en.wikipedia.org/wiki/Zxx)
     * @param lang language code to check
     * @return true if provided language is zxx, else false
     */
    public static boolean isNoLinguisticContent(String lang) {
        return NO_LINGUISTIC_CONTENT.equalsIgnoreCase(lang);
    }

    /**
     * Return true, if lang value is with regions ex: en-GB
     * @param lang
     * @return
     */
    private static boolean isLanguageWithRegionLocales(String lang) {
        return lang.length() > 2 && lang.contains("-") ;
    }

    /**
     * returns the substring  before '-' if lang value is with region locales
     * @param lang
     * @return
     */
    public static String stripLangStringIfRegionPresent(String lang) {
        if (isLanguageWithRegionLocales(lang)) {
            return StringUtils.substringBefore(lang, "-");
        }
        return lang;
    }
}
