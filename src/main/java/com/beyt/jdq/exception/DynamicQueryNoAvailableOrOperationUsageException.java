package com.beyt.jdq.exception;


/**
 * Created by tdilber at 24-Aug-19
 */
public class DynamicQueryNoAvailableOrOperationUsageException extends RuntimeException {

    public DynamicQueryNoAvailableOrOperationUsageException(String errorMessage) {
        super(errorMessage);
    }

    public DynamicQueryNoAvailableOrOperationUsageException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
