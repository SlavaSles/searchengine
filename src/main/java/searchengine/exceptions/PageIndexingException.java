package searchengine.exceptions;

import searchengine.exceptions.errorMessage.ErrorMessage;

public class PageIndexingException extends RuntimeException {
    public PageIndexingException() {
        super(ErrorMessage.PAGE_INDEXING_ERROR);
    }
}