package searchengine.exceptions;

import searchengine.exceptions.errorMessage.ErrorMessage;

public class StopIndexingException extends RuntimeException {
    public StopIndexingException() {
        super(ErrorMessage.STOP_INDEXING_ERROR.getMessage());
    }
}