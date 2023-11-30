package searchengine.exceptions;

import searchengine.exceptions.errorMessage.ErrorMessage;

public class SiteNotIndexedException extends RuntimeException {
    public SiteNotIndexedException() {
        super(ErrorMessage.SITE_NOT_INDEXED);
    }
}