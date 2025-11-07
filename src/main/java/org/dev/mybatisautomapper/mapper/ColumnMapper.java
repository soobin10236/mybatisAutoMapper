package org.dev.mybatisautomapper.mapper;

import org.dev.mybatisautomapper.model.ColumnInfo;

import java.util.List;

public interface ColumnMapper {
    List<ColumnInfo> selectColumns(String tableName);

    List<String> selectAllTableNames();
}
