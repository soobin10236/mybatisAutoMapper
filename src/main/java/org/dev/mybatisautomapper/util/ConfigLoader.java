package org.dev.mybatisautomapper.util;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.Reader;

public class ConfigLoader {
    private static Config config;

    public static Config load(String path) {
        if (config == null) {
            try (Reader reader = new FileReader(path)) {
                config = new Gson().fromJson(reader, Config.class);
            } catch (Exception e) {
                throw new RuntimeException("config.json 로딩 실패!", e);
            }
        }
        return config;
    }
}