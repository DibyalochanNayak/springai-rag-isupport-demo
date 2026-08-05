package com.genai.advrag.isupport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NO_CONTENT, reason = "Web page is empty")
public class EmptyWebPageException extends RuntimeException {
    public EmptyWebPageException(String url) {
        super("Web page is empty: " + url);
    }
}
