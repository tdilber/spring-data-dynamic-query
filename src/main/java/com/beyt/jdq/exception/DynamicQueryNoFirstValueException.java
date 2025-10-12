package com.beyt.jdq.exception;

/**
 * Created by tdilber at 24-Aug-19
 */
public class DynamicQueryNoFirstValueException extends RuntimeException {

    public DynamicQueryNoFirstValueException(String errorMessage) {
        super(errorMessage);
    }

    public DynamicQueryNoFirstValueException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
