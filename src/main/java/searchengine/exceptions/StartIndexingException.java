package searchengine.exceptions;

import searchengine.exceptions.errorMessage.ErrorMessage;

public class StartIndexingException extends RuntimeException {
    public StartIndexingException() {
        super(ErrorMessage.START_INDEXING_ERROR.getMessage());
    }
}