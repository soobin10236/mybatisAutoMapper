package org.dev.mybatisautomapper.dao;

import org.apache.ibatis.session.SqlSession;
import org.dev.mybatisautomapper.mapper.ColumnMapper;
import org.dev.mybatisautomapper.model.ColumnInfo;
import org.dev.mybatisautomapper.util.MyBatisUtil;

import java.util.List;

public class ColumnDao {
    // SqlSession을 직접 매개변수로 받아서 트랜잭션 관리를 Service 계층에 위임하거나
    // 각 DAO 메서드 내에서 SqlSession을 열고 닫는 방법 (현재 Service 방식과 유사)
    public List<ColumnInfo> selectColumnsByTableName(String tableName) {
        try (SqlSession session = MyBatisUtil.getFactory().openSession()) {
            // MyBatis Mapper 인터페이스를 사용하여 SQL 실행
            return session.getMapper(ColumnMapper.class).selectColumns(tableName);
        }
    }

    /**
     * 현재 스키마의 모든 테이블 목록을 조회합니다.
     * @return 테이블 이름 리스트
     */
    public List<String> selectAllTableNames() {
        try (SqlSession session = MyBatisUtil.getFactory().openSession()) {
            // "namespace.id" 형식으로 호출 (namespace는 실제 파일에 맞게 수정)
            return session.getMapper(ColumnMapper.class).selectAllTableNames();
        }
    }
}
