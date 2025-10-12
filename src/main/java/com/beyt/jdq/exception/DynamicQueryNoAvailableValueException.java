package com.beyt.jdq.exception;

/**
 * Created by tdilber at 24-Aug-19
 */
public class DynamicQueryNoAvailableValueException extends RuntimeException {
    public DynamicQueryNoAvailableValueException(String errorMessage) {
        super(errorMessage);
    }

    public DynamicQueryNoAvailableValueException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
