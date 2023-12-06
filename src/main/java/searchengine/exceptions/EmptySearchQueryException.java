package searchengine.exceptions;

import searchengine.exceptions.errorMessage.ErrorMessage;

public class EmptySearchQueryException extends IllegalArgumentException {
    public EmptySearchQueryException() {
        super(ErrorMessage.EMPTY_SEARCH_QUERY.getMessage());
    }
}