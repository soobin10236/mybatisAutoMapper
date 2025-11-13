package org.dev.mybatisautomapper.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dev.mybatisautomapper.dao.ColumnDao;
import org.dev.mybatisautomapper.model.ColumnInfo;
import org.dev.mybatisautomapper.util.MyBatisUtil;

import java.sql.*;
import java.util.List;

public class TableInfoService {
    private static final Logger logger = LogManager.getLogger(TableInfoService.class);

    // DAO 인스턴스
    private final ColumnDao columnDao; // final로 선언하고 생성자 주입을 고려

    public TableInfoService() {
        this.columnDao = new ColumnDao(); // 간단하게 인스턴스 생성. (Spring 등에서는 DI로 주입)
    }

    /**
     * DB 연결 테스트를 MyBatisUtil로 위임
     * @return 연결 성공 정보 String, 실패 시 예외 발생
     */
    public String checkDbConnection() throws Exception { // testConnection()에서 이름 변경 (MyBatisUtil의 testConnection과 혼동 방지)

        return MyBatisUtil.testConnection(); // MyBatisUtil의 testConnection() 호출
    }

    /** 2) 컬럼 정보 조회 - DAO를 사용하여 데이터 접근 */
    public List<ColumnInfo> fetchColumns(String tableName) {
        logger.debug("[DB] Fetching columns for table: {}", tableName);
        // DAO의 메서드를 호출하여 데이터 접근 로직 위임
        List<ColumnInfo> columns = columnDao.selectColumnsByTableName(tableName);

        logger.debug("[DB] Fetched {} columns for table {}", columns.size(), tableName);
        return columns;
    }

    /**
     * DB에서 모든 테이블 이름 목록을 가져옵니다.
     * @return 테이블 이름 리스트
     */
    public List<ColumnInfo> getAllTableNames() throws Exception {
        return columnDao.selectAllTableNames();
    }
}
