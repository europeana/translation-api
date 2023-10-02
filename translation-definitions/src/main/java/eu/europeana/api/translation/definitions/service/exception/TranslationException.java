package eu.europeana.api.translation.definitions.service.exception;

/**
 * Exception that is thrown when there is an error using the translation service
 */
public class TranslationException extends Exception {

  /**
  * 
  */
  private static final long serialVersionUID = -1787377732687111908L;
  private int remoteStatusCode;


  /**
   * Constructor for exception to indicate that an error occurred during invocation of the remote
   * service or parsing of service response
   * 
   * @param msg the error message
   * @param remoteStatusCode the status code from the remote service
   * @param t eventual exception thrown when extracting information from the remote service response
   */
  public TranslationException(String msg, int remoteStatusCode, Throwable t) {
    super(msg, t);
    this.remoteStatusCode = remoteStatusCode;
  }

  /**
   * Constructor for exception to indicate that an error occurred during invocation of the remote
   * service or parsing of service response
   * 
   * @param msg the error message
   * @param remoteStatusCode the status code from the remote service
   */
  public TranslationException(String msg, int remoteStatusCode) {
    super(msg);
    this.remoteStatusCode = remoteStatusCode;
  }
  
  /**
   * Constructor for exception to indicate that an error occurred during invocation of the remote
   * service when no response is received from the remote server 
   * 
   * @param msg the error message
   * @param remoteStatusCode the status code from the remote service
   */
  public TranslationException(String msg) {
    this(msg, -1);
  }

  public int getRemoteStatusCode() {
    return remoteStatusCode;
  }

  public void setRemoteStatusCode(int remoteStatusCode) {
    this.remoteStatusCode = remoteStatusCode;
  }
}
