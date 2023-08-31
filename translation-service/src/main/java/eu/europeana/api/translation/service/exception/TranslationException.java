package eu.europeana.api.translation.service.exception;

/**
 * Exception that is thrown when there is an error using the translation service
 */
public class TranslationException extends Exception{

   /**
   * 
   */
  private static final long serialVersionUID = -1787377732687111908L;

  public TranslationException(String msg, Throwable t) {
    super(msg, t);
  }
  
  public TranslationException(String msg) {
    super(msg);
  }
}
