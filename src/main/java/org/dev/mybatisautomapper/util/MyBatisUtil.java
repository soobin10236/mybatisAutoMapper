package org.dev.mybatisautomapper.util;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyBatisUtil {
    private static final Logger logger = LogManager.getLogger(MyBatisUtil.class);

    private static SqlSessionFactory factory;

    public static SqlSessionFactory getFactory() {

        if (factory == null) {
            try (Reader reader = Resources.getResourceAsReader("mybatis-config.xml")) {
                logger.info("[DB] mybatis-config.xml: {}", reader);

                // 1) JSON에서 설정 로드
                Config cfg = ConfigLoader.load("config.json");
                Properties props = new Properties();
                props.setProperty("db.driver",   cfg.db.driver);
                props.setProperty("db.url",      cfg.db.url);
                props.setProperty("db.user",     cfg.db.user);
                props.setProperty("db.password", cfg.db.password);

                // 2) MyBatis 빌드 시에 props 전달
                factory = new SqlSessionFactoryBuilder().build(reader, props);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return factory;
    }

    /**
     * 1) DB 커넥션 테스트
     *
     * - SqlSessionFactory에서 세션을 열고 Connection.isValid(timeout) 호출
     * - 예외 발생 시 catch하여 false 반환
     *
     * @return 연결 유효 시 true, 아닐 시 false
     */
    public static boolean testConnection() {
        try (SqlSession session = MyBatisUtil.getFactory().openSession()) {
            logger.info("session 연결 시작 : {}", session);
            Connection conn = session.getConnection();
            boolean valid = conn.isValid(2);
            logger.info("[DB] Connection valid: {}", valid);
            return valid;
        } catch (Exception e) {
            logger.error("[DB] Connection test failed", e);
            return false;
        }
    }
}
