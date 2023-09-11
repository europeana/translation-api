package eu.europeana.api.translation;

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
public class TranslationApp{

  /**
   * Main entry point of this application
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    // start the application
    SpringApplication.run(TranslationApp.class, args);
  }
}
