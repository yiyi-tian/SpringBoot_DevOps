package org.example.topbiz.exception;

public class InternalServiceException extends RuntimeException {

    private final int code;

    public InternalServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
