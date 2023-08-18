package eu.europeana.api.translation;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.mongo.MongoMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import eu.europeana.api.translation.config.BeanNames;
import eu.europeana.api.translation.config.TranslationServiceConfigProvider;

/**
 * Main application. Allows deploying as a war and logs instance data when deployed in Cloud Foundry
 */
@SpringBootApplication(scanBasePackages = {"eu.europeana.api.translation"}, exclude = {
    // Remove these exclusions to re-enable security
    SecurityAutoConfiguration.class,
    // WebMvcAutoConfiguration.class,
    MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
    EmbeddedMongoAutoConfiguration.class, EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
    MongoMetricsAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class,
    SolrAutoConfiguration.class,
    // DataSources are manually configured (for EM and batch DBs)
    DataSourceAutoConfiguration.class})
public class TranslationApp extends SpringBootServletInitializer {

  private static final Logger logger = LogManager.getLogger(TranslationApp.class);

  /**
   * Main entry point of this application
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    //start the application
    ConfigurableApplicationContext ctx = SpringApplication.run(TranslationApp.class, args);

    //log beans for debuging purposes
    if (logger.isDebugEnabled()) {
      printRegisteredBeans(ctx);
    }
    
    //init translation services
    try {
      initTranslationServices(ctx);
    } catch (Exception e) {
      //gracefully stop the application in case of configuration problems (code 1 means exception occured at startup)
      logger.fatal("Stopping application. Translation Service initialization failed due to configuration errors!", e);
      System.exit(SpringApplication.exit(ctx, () -> 1));
    }
  }

  private static void initTranslationServices(ApplicationContext ctx) throws Exception {
    TranslationServiceConfigProvider translationServiceProvider = (TranslationServiceConfigProvider) ctx
        .getBean(BeanNames.BEAN_SERVICE_CONFIG_PROVIDER);
    translationServiceProvider.initTranslationServicesConfiguration();
  }

  private static void printRegisteredBeans(ApplicationContext ctx) {
    String[] beanNames = ctx.getBeanDefinitionNames();
    Arrays.sort(beanNames);
    logger.debug("Instantiated beans:");
    logger.debug(StringUtils.join(beanNames, "\n"));
  }
}
