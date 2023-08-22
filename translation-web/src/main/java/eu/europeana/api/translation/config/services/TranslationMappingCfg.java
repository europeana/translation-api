package eu.europeana.api.translation.config.services;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.europeana.api.translation.definitions.language.LanguagePair;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({TranslationAppConstants.SOURCE_LANG, TranslationAppConstants.TARGET_LANG, TranslationAppConstants.SERVICE})
public class TranslationMappingCfg {

    private List<String> srcLang;
    private List<String> trgLang;
    private String serviceId;

    public TranslationMappingCfg() {
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
    public List<String> getTrgLang() {
      return trgLang;
    }

    @JsonSetter(TranslationAppConstants.TARGET_LANG)
    public void setTrgLang(List<String> trgLang) {
      this.trgLang = trgLang;
    }

    @JsonGetter(TranslationAppConstants.SERVICE)
    public String getServiceId() {
      return serviceId;
    }

    @JsonSetter(TranslationAppConstants.SERVICE)
    public void setServiceId(String serviceId) {
      this.serviceId = serviceId;
    }
    
    public boolean isSupported(LanguagePair languagePair){
      return getTrgLang().contains(languagePair.getTargetLang()) && getSrcLang().contains(languagePair.getTargetLang());
    }
}
