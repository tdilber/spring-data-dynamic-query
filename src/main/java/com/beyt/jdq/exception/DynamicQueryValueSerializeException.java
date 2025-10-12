package com.beyt.jdq.exception;

/**
 * Created by tdilber at 24-Aug-19
 */
public class DynamicQueryValueSerializeException extends RuntimeException {
    public DynamicQueryValueSerializeException(String errorMessage) {
        super(errorMessage);
    }

    public DynamicQueryValueSerializeException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
