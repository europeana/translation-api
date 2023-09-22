package eu.europeana.api.translation.definitions.service.exception;

/**
 * Exception that is thrown when there is an error using the translation service
 */
public class LanguageDetectionException extends Exception{

   /**
   * 
   */
  private static final long serialVersionUID = -1787377732687111908L;

  public LanguageDetectionException(String msg, Throwable t) {
    super(msg, t);
  }
  
  public LanguageDetectionException(String msg) {
    super(msg);
  }
}
