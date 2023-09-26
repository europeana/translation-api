package eu.europeana.api.translation.definitions.service.exception;

/**
 * Exception that is thrown when there is an error using the translation service
 */
public class LanguageDetectionException extends Exception{

   /**
   * 
   */
  private static final long serialVersionUID = -1787377732687111908L;
  private int remoteStatusCode;

  public LanguageDetectionException(String msg, int remoteStatusCode, Throwable t) {
    super(msg, t);
    this.remoteStatusCode = remoteStatusCode;
  }
  
  public LanguageDetectionException(String msg, int remoteStatusCode) {
    super(msg);
    this.remoteStatusCode = remoteStatusCode;
  }

  public int getRemoteStatusCode() {
    return remoteStatusCode;
  }
}
