package eu.europeana.api.translation.language;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enum class for Pangeanic supported Languages and the threshold values for the languages during translations
 *
 * @author Srishti Singh
 * @since 6 April 2023
 */
public enum PangeanicLanguages {

    SK(0.8740), RO(0.8545), BG(0.8678), PL(0.8710), HR(0.8901),
    SV(0.7850), FR(0.8650), IT(0.8246), ES(0.8213), CS(0.8410),
    DE(0.8288), LV(0.7884), NL(0.7757), EL(0.8009), FI(0.7975),
    DA(0.7548), SL(0.7514), HU(0.7779), PT(0.7243), ET(0.7424),
    LT(0.6538), GA(0.8650);

    private final double translationThresholds;

    protected static final Logger LOG = LogManager.getLogger(PangeanicLanguages.class);

    PangeanicLanguages(double translationThresholds) {
        this.translationThresholds = translationThresholds;
    }

    public double getTranslationThresholds() {
        return translationThresholds;
    }

    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Stream.of(PangeanicLanguages.values())
            .map(Enum::name)
            .collect(Collectors.toList()));

    private static final List<LanguagePair> TRANSLATION_PAIRS = new ArrayList<>(Stream.of(PangeanicLanguages.values())
            .map(e -> new LanguagePair(e.name(), Language.EN.name()))
            .collect(Collectors.toList()));

    /**
     * Returns the threshold value for the Language.
     *
     * @param lang
     * @return
     */
    public static double getThresholdForLanguage(String lang) {
        for (PangeanicLanguages e : values()) {
            if (StringUtils.equalsIgnoreCase(e.name(), lang)) {
                return e.getTranslationThresholds();
            }
        }
        return 0.0;
    }

    /**
     * Returns true is the provided language is supported by Pangeanic
     *
     * @param language
     * @return
     */
    public static boolean isLanguageSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(language.toUpperCase(Locale.ROOT));
    }


    /**
     * Returns true if Language pair is supported for Pangeanic Translation
     *
     * @param srourceLang source langauge
     * @param targetLang  target lanaguge. Always "en" in the case of Pangeanic
     * @return
     */
    public static boolean isLanguagePairSupported(String srourceLang, String targetLang) {
        if (!StringUtils.equals(targetLang, Language.ENGLISH)) {
            LOG.error("For Pangeanic Translations target language must always be 'en' - {}", targetLang);
            return false;
        }
        return TRANSLATION_PAIRS.contains(new LanguagePair(srourceLang.toUpperCase(Locale.ROOT), targetLang.toUpperCase(Locale.ROOT)));
    }
}
