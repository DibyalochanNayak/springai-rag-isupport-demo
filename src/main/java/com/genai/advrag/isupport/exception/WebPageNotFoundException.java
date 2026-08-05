package com.genai.advrag.isupport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Web page not found")
public class WebPageNotFoundException extends RuntimeException {
    public WebPageNotFoundException(String url) {
        super("Web page not found: " + url);
    }
}
