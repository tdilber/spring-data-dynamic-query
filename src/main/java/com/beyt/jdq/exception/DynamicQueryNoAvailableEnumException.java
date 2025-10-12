package com.beyt.jdq.exception;

/**
 * Created by tdilber at 24-Aug-19
 */
public class DynamicQueryNoAvailableEnumException extends RuntimeException {

    public DynamicQueryNoAvailableEnumException(String errorMessage) {
        super(errorMessage);
    }

    public DynamicQueryNoAvailableEnumException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
