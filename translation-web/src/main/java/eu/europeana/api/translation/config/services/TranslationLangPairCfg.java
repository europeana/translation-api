package eu.europeana.api.translation.config.services;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.SOURCE_LANG, TranslationAppConstants.TARGET_LANG})
public class TranslationLangPairCfg {

    private List<String> srcLang;
    private List<String> targetLang;

    public TranslationLangPairCfg() {
      super();
    }

    @JsonGetter(TranslationAppConstants.SOURCE_LANG)
    public List<String> getSrcLang() {
      return srcLang;
    }
    
    @JsonSetter(TranslationAppConstants.SOURCE_LANG)
    public void setSrcLang(List<String> srcLang) {
      this.srcLang = srcLang;
    }

    @JsonGetter(TranslationAppConstants.TARGET_LANG)
    public List<String> getTargetLang() {
      return targetLang;
    }

    @JsonSetter(TranslationAppConstants.TARGET_LANG)
    public void setTargetLang(List<String> targetLang) {
      this.targetLang = targetLang;
    }

}
