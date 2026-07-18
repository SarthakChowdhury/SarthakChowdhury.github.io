package com.example.inference.core;

public class InferenceRateLimitException extends RuntimeException {
    public InferenceRateLimitException(String message) {
        super(message);
    }
}
