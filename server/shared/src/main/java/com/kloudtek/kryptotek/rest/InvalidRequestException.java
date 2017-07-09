package com.kloudtek.kryptotek.rest;

/**
 * Created by yannick on 6/25/17.
 */
public class InvalidRequestException extends Exception {
    private Object requestObject;

    public InvalidRequestException(Object requestObject) {
        this.requestObject = requestObject;
    }

    public InvalidRequestException(String message, Object requestObject) {
        super(message);
        this.requestObject = requestObject;
    }

    public InvalidRequestException(String message, Throwable cause, Object requestObject) {
        super(message, cause);
        this.requestObject = requestObject;
    }

    public InvalidRequestException(Throwable cause, Object requestObject) {
        super(cause);
        this.requestObject = requestObject;
    }
}
