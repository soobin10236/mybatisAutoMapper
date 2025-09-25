package org.dev.mybatisautomapper.model;

public class ColumnInfo {
    private String table_nm;
    private String table_comments;
    private String column_name;
    private String column_comments;
    private String data_type;
    private String primary_key;

    public String getTable_nm() {
        return table_nm;
    }

    public void setTable_nm(String table_nm) {
        this.table_nm = table_nm;
    }

    public String getTable_comments() {
        return table_comments;
    }

    public void setTable_comments(String table_comments) {
        this.table_comments = table_comments;
    }

    public String getColumn_name() {
        return column_name;
    }

    public void setColumn_name(String column_name) {
        this.column_name = column_name;
    }

    public String getColumn_comments() {
        return column_comments;
    }

    public void setColumn_comments(String column_comments) {
        this.column_comments = column_comments;
    }

    public String getData_type() {
        return data_type;
    }

    public void setData_type(String data_type) {
        this.data_type = data_type;
    }

    public String getPrimary_key() {
        return primary_key;
    }

    public void setPrimary_key(String primary_key) {
        this.primary_key = primary_key;
    }

    @Override
    public String toString() {
        return "ColumnInfo{" +
                "table_nm='" + table_nm + '\'' +
                ", table_comments='" + table_comments + '\'' +
                ", column_name='" + column_name + '\'' +
                ", column_comments='" + column_comments + '\'' +
                ", data_type='" + data_type + '\'' +
                ", primary_key='" + primary_key + '\'' +
                '}';
    }
}
