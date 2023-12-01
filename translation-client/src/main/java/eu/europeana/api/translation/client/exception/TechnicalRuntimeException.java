package eu.europeana.api.translation.client.exception;


public class TechnicalRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 8281933808897246375L;

    public TechnicalRuntimeException(String message, Exception e) {
        super(message, e);
    }

    public TechnicalRuntimeException(String message) {
        super(message);
    }
}
