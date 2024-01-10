package eu.europeana.api.translation.web.exception;

/**
 * Class used to indicate configuration exceptions which may prevent correct initialization  
 */
public class AppConfigurationException extends Exception {

  
  private static final long serialVersionUID = -1733123054365537665L;

  public AppConfigurationException(String message) {
    super (message);
  }
}
