package org.dev.mybatisautomapper.service;

import okhttp3.*;
import java.io.IOException;

public class OpenAiService implements AiService{
    private final String endpoint, apiKey, model;

    public OpenAiService(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(String prompt) throws Exception {
        OkHttpClient client = new OkHttpClient();
        String json = "{"
                + "\"model\":\""+model+"\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\""+prompt+"\"}]"
                + "}";
        Request req = new Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer "+apiKey)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (Response res = client.newCall(req).execute()) {
            // 간단히 첫 메시지 리턴 (실제 파싱 로직 추가 필요)
            return res.body().string();
        }
    }
}
