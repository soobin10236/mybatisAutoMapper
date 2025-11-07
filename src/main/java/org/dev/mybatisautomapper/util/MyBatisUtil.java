package org.dev.mybatisautomapper.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
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
                props.setProperty("db.driver",   cfg.getDb().driver);
                props.setProperty("db.url",      cfg.getDb().url);
                props.setProperty("db.user",     cfg.getDb().user);
                props.setProperty("db.password", cfg.getDb().password);

                // 2) MyBatis 빌드 시에 props 전달
                factory = new SqlSessionFactoryBuilder().build(reader, props);

                // 3) 연결이 성공했을 때, DB가 Oracle인지 확인
                // (이 메서드 내부에서 session.getConnection()을 호출하며, 이 때 "No suitable driver" 에러가 발생할 수 있음)
                validateDatabaseType(factory);

            } catch (Exception e) {
                // 팩토리 생성 실패 시, 다음 호출을 위해 null로 유지
                factory = null;
                logger.error("[DB] MyBatis 팩토리 생성 또는 DB 검증에 실패했습니다.", e);
                // 런타임 예외를 발생시켜 애플리케이션 시작을 중단
                // ================== 예외 원인 분석 ==================

                // 예외의 가장 근본적인 원인(Root Cause)을 찾습니다.
                Throwable rootCause = e;
                while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                    rootCause = rootCause.getCause();
                }

                // 원인이 "No suitable driver"인지 확인합니다. (Tibero 등 다른 드라이버가 없는 경우)
                if (rootCause instanceof SQLException && rootCause.getMessage() != null && rootCause.getMessage().toLowerCase().contains("no suitable driver")) {
                    // 사용자에게 친절한 에러 메시지를 생성하여 던집니다.
                    throw new RuntimeException(
                        "Mybatis Auto Mapper는 Oracle DB 전용입니다."
                    );
                }

                // 그 외 모든 에러 (예: "config.json" 없음, IP/Port 접속 실패, 아이디/암호 틀림 등)
                throw new RuntimeException("DB 초기화 실패: " + e.getMessage(), e);
            }
        }
        return factory;
    }

    /**
     * 생성된 SqlSessionFactory를 사용해 DB 연결을 테스트하고 Oracle DB인지 검증합니다.
     */
    private static void validateDatabaseType(SqlSessionFactory factory) {
        logger.info("[DB] 연결된 데이터베이스 유형 검증을 시작합니다...");
        try (SqlSession session = factory.openSession()) {
            Connection conn = session.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProductName = metaData.getDatabaseProductName();

            logger.info("[DB] 현재 연결된 데이터베이스: {}", dbProductName);

            // DB 제품 이름에 "oracle"이 포함되어 있는지 확인
            if (dbProductName == null || !dbProductName.toLowerCase().contains("oracle")) {
                // Oracle이 아니면, 에러를 발생시켜 팩토리 생성을 중단시킵니다.
                throw new RuntimeException(
                    "지원하지 않는 데이터베이스입니다: " + dbProductName + "\n" +
                    "Mybatis Auto Mapper는 Oracle 데이터베이스 전용입니다."
                );
            }
            logger.info("[DB] Oracle 데이터베이스가 확인되었습니다. 검증 성공.");

        } catch (Exception e) {
            // 이 예외를 다시 던져서 getFactory()의 catch 블록에서 잡도록 합니다.
            throw new RuntimeException("DB 유형 검증 중 오류가 발생했습니다.", e);
        }
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
            // getFactory()가 의도적으로 발생시킨 RuntimeException인지 확인합니다.
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e; // 예외를 다시 던져서 Task의 failed()가 받도록 함
            }

            logger.error("[DB] Connection test failed", e);
            return false;
        }
    }
}
