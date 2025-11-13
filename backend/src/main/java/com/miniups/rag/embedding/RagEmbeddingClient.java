package com.miniups.rag.embedding;

import java.util.List;

public interface RagEmbeddingClient {

    List<float[]> embed(List<String> inputs);

    int embeddingDimensions();
}
