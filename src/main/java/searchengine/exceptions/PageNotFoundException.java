package searchengine.exceptions;

import searchengine.exceptions.errorMessage.ErrorMessage;

import java.util.NoSuchElementException;

public class PageNotFoundException extends NoSuchElementException {
    public PageNotFoundException() {
        super(ErrorMessage.PAGE_NOT_FOUND.getMessage());
    }
}