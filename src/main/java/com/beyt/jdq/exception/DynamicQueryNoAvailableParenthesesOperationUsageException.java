package com.beyt.jdq.exception;


/**
 * Created by tdilber at 24-Aug-19
 */
public class DynamicQueryNoAvailableParenthesesOperationUsageException extends RuntimeException {

    public DynamicQueryNoAvailableParenthesesOperationUsageException(String errorMessage) {
        super(errorMessage);
    }

    public DynamicQueryNoAvailableParenthesesOperationUsageException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
