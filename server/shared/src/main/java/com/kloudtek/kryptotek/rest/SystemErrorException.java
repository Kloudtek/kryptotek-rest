package com.kloudtek.kryptotek.rest;

/**
 * Created by yannick on 6/25/17.
 */
public class SystemErrorException extends Exception {
    public SystemErrorException() {
    }

    public SystemErrorException(String message) {
        super(message);
    }

    public SystemErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemErrorException(Throwable cause) {
        super(cause);
    }
}
