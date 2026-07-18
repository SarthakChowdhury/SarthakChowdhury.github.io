package com.example.inference.model;

import java.util.List;

public record BatchInferenceRequest(List<String> prompts) {
}
