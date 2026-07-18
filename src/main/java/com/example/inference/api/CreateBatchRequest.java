package com.example.inference.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateBatchRequest {
    @NotEmpty
    @Size(max = 1000)
    private List<@NotEmpty String> prompts;

    public List<String> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<String> prompts) {
        this.prompts = prompts;
    }
}
