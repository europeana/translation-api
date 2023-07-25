package eu.europeana.api.translation.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.config.serialization.DetectServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationLangPairCfg;
import eu.europeana.api.translation.config.serialization.TranslationMappingCfg;
import eu.europeana.api.translation.config.serialization.TranslationServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationServicesConfiguration;
import eu.europeana.api.translation.definitions.vocabulary.TranslationAppConstants;
import eu.europeana.api.translation.service.LanguageDetectionService;
import eu.europeana.api.translation.service.TranslationService;
import eu.europeana.api.translation.web.exception.TranslationException;

@Component
public class InitServicesGlobalJsonConfig implements ApplicationListener<ContextRefreshedEvent> {

  @Value("${translation.config.file}")
  private String translConfigFile;
  
  @Autowired ApplicationContext applicationContext;

  TranslationServicesConfiguration appGlobalJsonConfig;
  Map<String, LanguageDetectionService> langDetectServices = new HashMap<>();
  Map<String, TranslationService> translServices = new HashMap<>();

  public TranslationServicesConfiguration getAppGlobalJsonConfig() {
    return appGlobalJsonConfig;
  }
  
  public Map<String, LanguageDetectionService> getLangDetectServices() {
    return langDetectServices;
  }
  
  public Map<String, TranslationService> getTranslServices() {
    return translServices;
  }

  //executed after all beans are initialized, to initialize the services from the main json config file
  @Override public void onApplicationEvent(ContextRefreshedEvent event) {
    try {
      initServicesGlobalJsonConfig();
    } catch (Exception e) {
      throw new RuntimeException("The initialization of the services from the global json config has failed.", e);
    }
  }
  
  private void initServicesGlobalJsonConfig() throws Exception {
    InputStream inputStream = getClass().getResourceAsStream(translConfigFile);
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    appGlobalJsonConfig = new ObjectMapper().readValue(content, TranslationServicesConfiguration.class);
    
    /*
     * Validate lang detection config
     */
    for(DetectServiceCfg detectServiceCfg : appGlobalJsonConfig.getLangDetectConfig().getServices()) {
      //validate unique service ids
      if(langDetectServices.containsKey(detectServiceCfg.getId())) {
        throw new TranslationException("Duplicate service id in the language detection config.");
      }
      LanguageDetectionService detectService = (LanguageDetectionService) applicationContext.getBean(Class.forName(detectServiceCfg.getClassname()));
      langDetectServices.put(detectServiceCfg.getId(), detectService);
    }  
    //check that a default service id is a valid one
    if(! langDetectServices.containsKey(appGlobalJsonConfig.getLangDetectConfig().getDefaultServiceId())) {
      throw new TranslationException("Language detection default service id is invalid.");
    }    
    //validate that the default service supports all languages from the supported section
    for(String supportedLang : appGlobalJsonConfig.getLangDetectConfig().getSupported()) {
      if(! langDetectServices.get(appGlobalJsonConfig.getLangDetectConfig().getDefaultServiceId()).isSupported(supportedLang)) {
        throw new TranslationException("The default language detection service does not support language: " + supportedLang + ", declared in the supported section");
      }
    }    

    /*
     * Validate translation config
     */
    for(TranslationServiceCfg translServiceConfig : appGlobalJsonConfig.getTranslConfig().getServices()) {
      //validate unique service ids
      if(translServices.containsKey(translServiceConfig.getId())) {
        throw new TranslationException("Duplicate service id in the translation config.");
      }
      TranslationService translService = (TranslationService) applicationContext.getBean(Class.forName(translServiceConfig.getClassname()));
      translServices.put(translServiceConfig.getId(), translService);
    }
    //check that a default service id is a valid one
    if(! translServices.containsKey(appGlobalJsonConfig.getTranslConfig().getDefaultServiceId())) {
      throw new TranslationException("Translation default service id is invalid.");
    }    
    //validate that each service supports the languages declared in the mappings section
    List<String> allMappingsLangPairs = new ArrayList<>();
    for(TranslationMappingCfg translMapping : appGlobalJsonConfig.getTranslConfig().getMappings()) {
      if(translServices.get(translMapping.getServiceId())==null) {
        throw new TranslationException("Translation service id declared in the mappings is invalid.");
      }
      for(String srcLang : translMapping.getSrcLang()) {
        for(String trgLang : translMapping.getTrgLang()) {
          if(!srcLang.equals(trgLang) && !translServices.get(translMapping.getServiceId()).isSupported(srcLang, trgLang) ) {
            throw new TranslationException("Translation service: " + translMapping.getServiceId() + ", does not support the language pair: " + srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang + ", declared in the mappings section.");
          }
          allMappingsLangPairs.add(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang);
        }
      }
    }
    //validate all languages from the supported section are actually supported
    for(TranslationLangPairCfg langPair : appGlobalJsonConfig.getTranslConfig().getSupported()) {
      for(String srcLang : langPair.getSrcLang()) {
        for(String trgLang : langPair.getTrgLang()) {
          if(!srcLang.equals(trgLang) && !allMappingsLangPairs.contains(srcLang + TranslationAppConstants.LANG_DELIMITER + trgLang)
              && !translServices.get(appGlobalJsonConfig.getTranslConfig().getDefaultServiceId()).isSupported(srcLang, trgLang)) {
            throw new TranslationException("The translation services do not support all languages declared in the supported section.");
          }
        }
      }
    }
  }
  
}