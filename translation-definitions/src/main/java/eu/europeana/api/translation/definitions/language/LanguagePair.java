package eu.europeana.api.translation.definitions.language;

import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

/**
 * Class to hold the Language pair values supported by the Translation services
 * Mostly for the future Translation API, when we have more than one translation service
 *
 * @author Hugo
 * @since 5 Apr 2023
 */
public class LanguagePair implements Comparable<LanguagePair> {

    private String srcLang;
    private String targetLang;

    public LanguagePair(String srcLang, @NotNull String targetLang) {
        this.srcLang = srcLang;
        this.targetLang = targetLang;
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
        return StringUtils.equals(targetLang, pair.targetLang) 
            && StringUtils.equals(srcLang, pair.srcLang); 
    }

    @Override
    public int compareTo(LanguagePair pair) {
        int ret = targetLang.compareTo(pair.targetLang);
        if(ret == 0) {
          ret = StringUtils.compare(srcLang, pair.srcLang);
        }
        return ret;
    }
    
    @Override
      public String toString() {
        return srcLang + TranslationAppConstants.LANG_DELIMITER + targetLang;
      }
    
    @Override
      public int hashCode() {
        return srcLang==null ? targetLang.hashCode() : srcLang.hashCode() + targetLang.hashCode();
      }

    public String getTargetLang() {
      return targetLang;
    }
}
