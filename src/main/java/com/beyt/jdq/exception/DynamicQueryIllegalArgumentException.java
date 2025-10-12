package com.beyt.jdq.exception;

/**
 * Created by tdilber at 14-Seo-2024
 */
public class DynamicQueryIllegalArgumentException extends IllegalArgumentException {

    public DynamicQueryIllegalArgumentException(String errorMessage) {
        super(errorMessage);
    }

    public DynamicQueryIllegalArgumentException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
