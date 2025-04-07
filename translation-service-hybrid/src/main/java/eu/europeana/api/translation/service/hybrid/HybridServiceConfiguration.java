package eu.europeana.api.translation.service.hybrid;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Configuration class for the Hybrid lang detection
 * @author Nuno Freire
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HybridServiceConfiguration {

  private List<String> services;

  /**
   * Default constructor
   */
  public HybridServiceConfiguration() {
    super();
  }

  @JsonGetter(HybridServiceConstants.SERVICES)
  public List<String> getServices() {
    return services;
  }

  @JsonSetter(HybridServiceConstants.SERVICES)
  public void setServices(List<String> services) {
    this.services = services;
  }

}
