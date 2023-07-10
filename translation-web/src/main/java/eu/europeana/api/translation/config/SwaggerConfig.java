package eu.europeana.api.translation.config;

import java.util.Collections;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Configures Swagger on all requests. Swagger Json file is availabe at <hostname>/v2/api-docs and
 * at <hostname/v3/api-docs. Swagger UI is available at <hostname>/swagger-ui/
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

  private final BuildProperties buildInfo;
  private final GitProperties gitProperties;

  /**
   * Initialize Swagger with API build information
   *
   * @param buildInfo object for retrieving build information
   * @param gitProperties git properties file
   */
  public SwaggerConfig(BuildProperties buildInfo, GitProperties gitProperties) {
    this.buildInfo = buildInfo;
    this.gitProperties = gitProperties;
  }

  /**
   * Initialize Swagger Documentation
   *
   * @return Swagger Docket for this API
   */
  @Bean
  public Docket api() {
//    return new Docket(DocumentationType.OAS_30)
    return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo())
        .select()
        .apis(RequestHandlerSelectors.basePackage("eu.europeana.api.translation"))
        .paths(PathSelectors.any())
        .build();
   }

  private ApiInfo apiInfo() {
    return new ApiInfo(
        // buildInfo.getName() returns incorrect value
        buildInfo.get("project.name"),
        buildInfo.get("project.description"),
        // gitProperties.getCommitId() returns null
        buildInfo.getVersion() + "(build " + gitProperties.get("commit.id.abbrev") + ")",
        null,
        new Contact("API team", "https://api.europeana.eu", "api@europeana.eu"),
        "EUPL 1.2",
        "https://www.eupl.eu",
        Collections.emptyList());
  }
}
