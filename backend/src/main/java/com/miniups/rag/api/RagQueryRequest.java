package com.miniups.rag.api;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class RagQueryRequest {

    @NotBlank(message = "query must not be blank")
    private String query;

    private Map<String, Object> context = new HashMap<>();
}
