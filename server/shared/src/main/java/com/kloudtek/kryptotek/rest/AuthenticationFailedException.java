package com.kloudtek.kryptotek.rest;

/**
 * Created by yannick on 6/25/17.
 */
public class AuthenticationFailedException extends Exception {
    private Reason reason;
    private Object requestObj;

    public AuthenticationFailedException(Reason reason, Object requestObj) {
        this.reason = reason;
        this.requestObj = requestObj;
    }

    public AuthenticationFailedException(String message, Reason reason, Object requestObj) {
        super(message);
        this.reason = reason;
        this.requestObj = requestObj;
    }

    public AuthenticationFailedException(String message, Throwable cause, Reason reason, Object requestObj) {
        super(message, cause);
        this.reason = reason;
        this.requestObj = requestObj;
    }

    public AuthenticationFailedException(Throwable cause, Reason reason, Object requestObj) {
        super(cause);
        this.reason = reason;
        this.requestObj = requestObj;
    }

    public AuthenticationFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Reason reason, Object requestObj) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.reason = reason;
        this.requestObj = requestObj;
    }

    public Reason getReason() {
        return reason;
    }

    public Object getRequestObj() {
        return requestObj;
    }

    public enum Reason {
        USER_NOT_FOUND, INVALID_SIGNATURE, USER_LOCKED
    }
}
