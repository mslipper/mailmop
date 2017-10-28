package com.mslipper.mailmop.exc;

public class MaximumRetriesException extends Exception {
    public MaximumRetriesException(String message, Throwable cause) {
        super(message, cause);
    }
}
