package eu.europeana.api.translation.service;

import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.api.translation.definitions.model.TranslationObj;

public abstract class AbstractTranslationService implements TranslationService {

  /**
   * sets the value of the translation with the text to be translated for translations objects in which the source language is the same as the target language 
   * @param translationObjs objects to process
   */
  protected void fillTranslationForSameLanguage(@NotNull List<TranslationObj> translationObjs) {
    for (TranslationObj translationObj : translationObjs) {
      fillTranslationForSameLanguage(translationObj);
    } 
  }

  /**
   * sets the value of the translation with the text to be translated if the source language is the same as the target language
   * @param translationObj object to process
   */
  protected void fillTranslationForSameLanguage(TranslationObj translationObj) {
    if(Objects.equals(translationObj.getTargetLang(), translationObj.getSourceLang())) {
      translationObj.setTranslation(translationObj.getText());
    }
  }
  
  /**
   * Sets the value of translation:
   *    - empty string for empty texts 
   *    - same value as the text, if the source language is the same as the target language  
   * @param translationObjs
   */
  protected void processNonTranslatable(List<TranslationObj> translationObjs) {
    for (TranslationObj translationObj : translationObjs) {
      if(StringUtils.isEmpty(translationObj.getText())){
        translationObj.setTranslation("");
      }
      
      fillTranslationForSameLanguage(translationObj);
    }
  }
}
