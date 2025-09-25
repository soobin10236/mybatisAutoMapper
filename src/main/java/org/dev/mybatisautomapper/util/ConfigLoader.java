package org.dev.mybatisautomapper.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public class ConfigLoader {
    private static Config config;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static Config load(String path) {
        if (config == null) {
            try (Reader reader = new FileReader(path)) {
                config = gson.fromJson(reader, Config.class);
            } catch (Exception e) {
                throw new RuntimeException("config.json 로딩 실패!", e);
            }
        }
        return config;
    }

    public static void save(String path) {
        if (config != null) {
            try (FileWriter writer = new FileWriter(path)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                e.printStackTrace(); // 예외 처리
            }
        }
    }
}