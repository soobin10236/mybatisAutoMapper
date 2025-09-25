package org.dev.mybatisautomapper.service;

import org.dev.mybatisautomapper.util.Config;

public class AiServiceFactory {
    public static AiService create(Config.Ai aiCfg) {
        Config.AiProvider p = aiCfg.providers.get(aiCfg.defaultProvider);
        switch (aiCfg.defaultProvider) {
            case "openai":
                return new OpenAiService(p.endpoint, p.apiKey, p.model);
            // case "huggingface": return new HuggingFaceService(...);
            default: throw new IllegalArgumentException("Unknown AI provider");
        }
    }
}
