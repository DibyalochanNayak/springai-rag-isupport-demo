package com.genai.advrag.isupport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND, reason="PDF is empty")
public class EmptyPdfException extends RuntimeException{

    public EmptyPdfException(String fileName) {
        super("PDF is empty : "+fileName);
    }
}
