package eu.europeana.api.translation.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.translation.config.serialization.DetectServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationServiceCfg;
import eu.europeana.api.translation.config.serialization.TranslationServicesConfiguration;
import eu.europeana.api.translation.web.service.LanguageDetectionService;
import eu.europeana.api.translation.web.service.TranslationService;

@Component
public class InitServicesGlobalJsonConfig implements ApplicationListener<ContextRefreshedEvent> {

  @Value("${translation.config.file}")
  private String translConfigFile;
  
  @Autowired ApplicationContext applicationContext;

  TranslationServicesConfiguration appGlobalJsonConfig;
  List<LanguageDetectionService> langDetectServices = new ArrayList<>();
  List<TranslationService> translServices = new ArrayList<>();

  public TranslationServicesConfiguration getAppGlobalJsonConfig() {
    return appGlobalJsonConfig;
  }
  
  public List<LanguageDetectionService> getLangDetectServices() {
    return langDetectServices;
  }
  
  public List<TranslationService> getTranslServices() {
    return translServices;
  }

  //executed after all beans are initialized, to initialize the services from the main json config file
  @Override public void onApplicationEvent(ContextRefreshedEvent event) {
    try {
      initServicesGlobalJsonConfig();
    } catch (Exception e) {
      throw new RuntimeException("The initialization of the services from the global json config has failed.");
    }
  }
  
  private void initServicesGlobalJsonConfig() throws Exception {
    InputStream inputStream = getClass().getResourceAsStream(translConfigFile);
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));

    appGlobalJsonConfig = new ObjectMapper().readValue(content, TranslationServicesConfiguration.class);
    for(DetectServiceCfg detectServiceCfg : appGlobalJsonConfig.getLangDetectConfig().getServices()) {
      LanguageDetectionService detectService = (LanguageDetectionService) applicationContext.getBean(Class.forName(detectServiceCfg.getClassname()));
      detectService.setSupportedLangs(detectServiceCfg.getSupportedLangs());
      langDetectServices.add(detectService);
    }    
    for(TranslationServiceCfg translServiceConfig : appGlobalJsonConfig.getTranslConfig().getServices()) {
      TranslationService translService = (TranslationService) applicationContext.getBean(Class.forName(translServiceConfig.getClassname()));
      translService.setSupportedLangs(translServiceConfig.getSupportedLangs());
      translServices.add(translService);
    }
  }
  
}