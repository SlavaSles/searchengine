package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.ErrorResponse;
import searchengine.exceptions.*;

@RestControllerAdvice
public class ApiErrorController {
    public static final String INTERNAL_ERROR = "Сбой работы приложения";

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(StartIndexingException.class)
    public ErrorResponse catchStartIndexingException(StartIndexingException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(StopIndexingException.class)
    public ErrorResponse catchStopIndexingException(StopIndexingException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PageIndexingException.class)
    public ErrorResponse catchPageIndexingException(PageIndexingException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EmptySearchQueryException.class)
    public ErrorResponse catchEmptySearchQueryException(EmptySearchQueryException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SiteNotIndexedException.class)
    public ErrorResponse catchSiteNotIndexedException(SiteNotIndexedException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PageNotFoundException.class)
    public ErrorResponse catchPageNotFoundException(PageNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(LemmatizerNotFoundException.class)
    public ErrorResponse catchLemmatizerNotFoundException(LemmatizerNotFoundException ex) {
        return new ErrorResponse(INTERNAL_ERROR);
    }
}
