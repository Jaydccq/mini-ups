package com.miniups.rag.generation;

public interface RagChatClient {

    String generate(String systemPrompt, String userPrompt, double temperature, int maxOutputTokens);
}
