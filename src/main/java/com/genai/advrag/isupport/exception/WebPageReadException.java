package com.genai.advrag.isupport.exception;

import org.jsoup.HttpStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Failed to read web page")
public class WebPageReadException extends RuntimeException {
    public WebPageReadException(String s, String exceptionMessage) {
        super("Failed to read web page: " + s + ". Exception message: " + exceptionMessage);
    }
}
