package org.dev.mybatisautomapper.util;

import java.util.Map;

public class Config {
    public static class Db {
        public String driver;
        public String url;
        public String user;
        public String password;
    }
    public static class AiProvider {
        public String apiKey;
        public String endpoint;
        public String model;
    }
    public static class Ai {
        public String defaultProvider;
        public Map<String, AiProvider> providers;
    }

    public Db db;
    public Ai ai;
}
