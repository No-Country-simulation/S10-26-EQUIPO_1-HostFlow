package com.hostflow.exception;

public class InvalidImageException extends BusinessException {

    public InvalidImageException(String message) {
        super(message, "INVALID_IMAGE");
    }
}
