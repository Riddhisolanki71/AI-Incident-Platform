package com.aiincident.exception;

/** Thrown when the AI analysis provider fails to return a usable result. */
public class AIAnalysisException extends RuntimeException {
    public AIAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public AIAnalysisException(String message) {
        super(message);
    }
}
