package com.example.inference.model;

import java.util.List;

public record BatchInferenceResponse(List<InferenceResult> results) {
}
