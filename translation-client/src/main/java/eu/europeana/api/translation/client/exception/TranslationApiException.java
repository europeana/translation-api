package eu.europeana.api.translation.client.exception;

public class TranslationApiException extends Exception {

    private static final long serialVersionUID = 8281933808897246375L;

    public TranslationApiException(String message, Exception e) {
        super(message, e);
    }

    public TranslationApiException(String message) {
        super(message);
    }
}