package com.insureguard.exception;

/** Raised when the Python analysis service (Watson/Roboflow) fails or is unreachable. */
public class AnalysisServiceException extends RuntimeException {
    public AnalysisServiceException(String message) {
        super(message);
    }

    public AnalysisServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
