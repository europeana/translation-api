package eu.europeana.api.translation.language;

/**
 * Class to hold the Language pair values supported by the Translation services
 * Mostly for the future Translation API, when we have more than one translation service
 *
 * @author Hugo
 * @since 5 Apr 2023
 */
public class LanguagePair implements Comparable<LanguagePair> {

    private String srcLang;
    private String trgLang;

    public LanguagePair(String srcLang, String trgLang) {
        this.srcLang = srcLang;
        this.trgLang = trgLang;
    }

    public String getSrcLang() {
        return srcLang;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LanguagePair)) {
            return false;
        }

        LanguagePair pair = (LanguagePair) obj;
        return srcLang.equals(pair.srcLang) && trgLang.equals(pair.trgLang);
    }

    @Override
    public int compareTo(LanguagePair pair) {
        int ret = srcLang.compareTo(pair.srcLang);
        return (ret != 0 ? ret : trgLang.compareTo(pair.trgLang));
    }
}
