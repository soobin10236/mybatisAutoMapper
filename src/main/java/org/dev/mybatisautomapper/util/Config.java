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

    public static class Ui {
        public String theme;
    }

    private Db db;
    private Ai ai;
    private Ui ui;

    public Db getDb() {
        return db;
    }

    public void setDb(Db db) {
        this.db = db;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public Ui getUi() {
        return ui;
    }

    public void setUi(Ui ui) {
        this.ui = ui;
    }
}
