package searchengine.exceptions;

import searchengine.exceptions.errorMessage.ErrorMessage;

public class LemmatizerNotFoundException extends RuntimeException {
    public LemmatizerNotFoundException() {
        super(ErrorMessage.LEMMATIZER_NOT_FOUND.getMessage());
    }
}