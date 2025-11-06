package org.dev.mybatisautomapper.util;

import org.dev.mybatisautomapper.model.ColumnInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MybatisMapperGenerator {

    private static final int COLUMNS_PER_LINE = 5; // 한 줄에 표시할 컬럼 수
    private static final String INDENT = "           "; // 기본 들여쓰기
    private static final int DEFAULT_PADDING = 4; // 각 컬럼 사이에 추가할 기본 공백 수

    /**
     * MyBatis SELECT 구문을 생성합니다. (PK 컬럼을 WHERE 절에 사용)
     * @param tableName 테이블 이름
     * @param columns 테이블 컬럼 정보 리스트
     * @return 생성된 SELECT XML 구문
     */
    public static String generateSelectStatement(String tableName, List<ColumnInfo> columns, boolean useIfWhere, boolean isParameterTypeModel) {
        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder selectClauseBuilder = new StringBuilder();
        StringBuilder whereBuilder = new StringBuilder();
        final String INDENT = "        "; // 들여쓰기용

        // WHERE 절에 사용할 PK 컬럼 필터링
        List<ColumnInfo> pkColumns = columns.stream()
                                            .filter(col -> "Y".equals(col.getPrimary_key()))
                                            .toList();

        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            // 컬럼 이름을 25자 너비로 왼쪽 정렬하여 포맷팅
            String columnNameFormatted = String.format("%-25s", col.getColumn_name());
            String comment = (col.getColumn_comments() != null && !col.getColumn_comments().isEmpty()) ? col.getColumn_comments() : "";

            if (i == 0) {
                selectClauseBuilder.append(INDENT).append("SELECT ").append(columnNameFormatted);
            } else {
                selectClauseBuilder.append(INDENT).append("     , ").append(columnNameFormatted);
            }

            // 코멘트가 있는 경우에만 '--코멘트' 형태로 추가
            if (!comment.isEmpty()) {
                selectClauseBuilder.append(" --").append(comment);
            }
            selectClauseBuilder.append("\n");
        }

        // --- WHERE 절 포매팅 ---
        whereBuilder.append(INDENT).append("<where>\n");
        for (ColumnInfo pkCol : pkColumns) {
            String columnName = pkCol.getColumn_name();
            String columnNameFormatted = String.format("%-" + 20 + "s", columnName);
            String finalParamName;

            // paramStyle 값에 따라 파라미터 형식을 분기 처리합니다.
            if (isParameterTypeModel) {
                // Model 스타일 (기본값): #{소문자컬럼명}
                finalParamName = lowerCaseSnakeCase(columnName);
            } else {
                // HashMap 스타일: #{P_대문자컬럼명}
                finalParamName = "P_" + columnName;
            }

            if(useIfWhere){
                whereBuilder.append(INDENT);
                String dataType = pkCol.getData_type();
                // 데이터 타입이 문자열(VARCHAR, CHAR) 계열인지 확인
                if (dataType.startsWith("VARCHAR") || dataType.startsWith("CHAR") || dataType.startsWith("NVARCHAR")) {
                    // 문자열 타입일 경우: null 체크 AND 빈 문자열 체크
                    whereBuilder.append("<if test=\"").append(finalParamName).append(" != null and ").append(finalParamName).append(" != ''\">\n");
                } else {
                    // NUMBER, DATE 등 그 외 모든 타입일 경우: null 체크만
                    whereBuilder.append("<if test=\"").append(finalParamName).append(" != null\">\n");
                }
            }
            whereBuilder.append(INDENT);
            whereBuilder.append("AND ").append(columnNameFormatted).append("= #{").append(finalParamName).append("}\n");
            if(useIfWhere){
                whereBuilder.append(INDENT);
                whereBuilder.append("</if>\n");
            }
        }
        whereBuilder.append(INDENT).append("</where>\n");

        // --- 최종 쿼리 조립 ---
        // <select> 태그를 사용하고, id와 resultType을 설정
        String parameterType = isParameterTypeModel ? "Model" : "HashMap";
        sqlBuilder.append("    <select id=\"select")
                  .append("\" parameterType=\"").append(parameterType).append("\" resultType=\"").append("Model").append("\">\n");
        sqlBuilder.append("        \n");
        sqlBuilder.append(selectClauseBuilder); // SELECT 절 추가
        sqlBuilder.append(INDENT).append("  FROM ").append(tableName).append("\n");
        sqlBuilder.append(whereBuilder);      // WHERE 절 추가
        sqlBuilder.append("        \n");
        sqlBuilder.append("    </select>");

        return sqlBuilder.toString();
    }

    /**
     * MyBatis INSERT 구문을 생성합니다.
     * @param tableName 테이블 이름
     * @param columns 테이블 컬럼 정보 리스트
     * @return 생성된 INSERT XML 구문
     */
    public static String generateInsertStatement(String tableName, List<ColumnInfo> columns, boolean isParameterTypeModel) {
        StringBuilder sqlBuilder = new StringBuilder();
        List<String> columnNames = new ArrayList<>();
        List<String> bindVariables = new ArrayList<>();

        // INSERT 문에 포함될 모든 컬럼을 대상으로 합니다.
        // 특정 규칙에 따라 자동 생성되는 컬럼 (INSERT_DTS 등)은 value 부분에서 특별 처리합니다.

        for (ColumnInfo col : columns) {
            String columnName = col.getColumn_name();
            if ("INSERT_IP".equals(columnName) ||
                "INSERT_MCADDR_NM".equals(columnName) ||
                "UPDATE_IP".equals(columnName) ||
                "UPDATE_MCADDR_NM".equals(columnName)) {
                continue; // 감사 컬럼은 건너뛰기
            }

            columnNames.add(columnName);

            String paramName; // 컬럼명 소문자로 변환
            if (isParameterTypeModel) {
                // Model 스타일 (기본값): #{소문자컬럼명}
                paramName = lowerCaseSnakeCase(columnName);
            } else {
                // HashMap 스타일: #{P_대문자컬럼명}
                paramName = "P_" + columnName;
            }

            String bindVar;

            if (col.getColumn_name().endsWith("INSERT_DTS")) {
                bindVar = "FN_TODATE(#{P_CM_SYSDATE})";
            } else if (col.getColumn_name().equals("INSERT_ID")) {
                bindVar = "#{P_INSERT_ID}";
            } else if (col.getColumn_name().equals("UPDATE_ID")) {
                bindVar = "#{P_UPDATE_ID}";
            } else if (col.getColumn_name().equals("UPDATE_DTS")) {
                bindVar = "FN_TODATE(#{P_CM_SYSDATE})";
            } else {
                bindVar = "#{" + paramName + "}";
            }
            bindVariables.add(bindVar);
        }

        // 컬럼명과 바인드 변수 라인별로 포매팅
        String formattedColumns = simplifiedFormatLineByLine(columnNames, COLUMNS_PER_LINE, ",", DEFAULT_PADDING);
        String formattedValues = simplifiedFormatLineByLine(bindVariables, COLUMNS_PER_LINE, ",", DEFAULT_PADDING);

        String parameterType = isParameterTypeModel ? "Model" : "HashMap";

        sqlBuilder.append("    \n");
        sqlBuilder.append("    <insert id=\"insert")
                  .append("\" parameterType=\"").append(parameterType).append("\">\n");
        sqlBuilder.append("        \n");
        sqlBuilder.append("        INSERT INTO ").append(tableName).append("\n");
        sqlBuilder.append("        ( ").append("\n");
        sqlBuilder.append(formattedColumns).append("\n"); // simplifiedFormatLineByLine 이미 들여쓰기 처리됨
        sqlBuilder.append("        )\n");
        sqlBuilder.append("        VALUES\n");
        sqlBuilder.append("        (\n");
        sqlBuilder.append(formattedValues).append("\n"); // simplifiedFormatLineByLine 이미 들여쓰기 처리됨
        sqlBuilder.append("        )\n");
        sqlBuilder.append("        \n");
        sqlBuilder.append("    </insert>");

        return sqlBuilder.toString();
    }

    /**
     * MyBatis UPDATE 구문을 생성합니다. (PK 컬럼을 WHERE 절에 사용)
     * @param tableName 테이블 이름
     * @param columns 테이블 컬럼 정보 리스트
     * @return 생성된 UPDATE XML 구문
     */
    public static String generateUpdateStatement(String tableName, List<ColumnInfo> columns, boolean useIfUpadate, boolean useIfWhere, boolean isParameterTypeModel) {
        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder setBuilder = new StringBuilder();
        StringBuilder whereBuilder = new StringBuilder();

        List<ColumnInfo> pkColumns = columns.stream()
                .filter(col -> "Y".equals(col.getPrimary_key()))
                .toList();



        List<ColumnInfo> updateableColumns = columns.stream()
                .filter(col -> !"Y".equals(col.getPrimary_key()) &&
                        !col.getColumn_name().equals("INSERT_ID") &&
                        !col.getColumn_name().equals("INSERT_IP") &&
                        !col.getColumn_name().equals("INSERT_MCADDR_NM") &&
                        !col.getColumn_name().equals("INSERT_DTS")
                )
                .toList();

        // SET 절 포매팅
        // <set> 태그 사용
        setBuilder.append(INDENT).append("<set>\n");

        for (ColumnInfo col : updateableColumns) {
            String paramName; // 컬럼명 소문자로 변환
            if (isParameterTypeModel) {
                // Model 스타일 (기본값): #{소문자컬럼명}
                paramName = lowerCaseSnakeCase(col.getColumn_name());
            } else {
                // HashMap 스타일: #{P_대문자컬럼명}
                paramName = "P_" + col.getColumn_name();
            }


            String columnNameFormatted = String.format("%-" + 20 + "s", col.getColumn_name());

            String columnName = col.getColumn_name();
            if ("UPDATE_ID".equals(columnName) ||
                "UPDATE_IP".equals(columnName) ||
                "UPDATE_MCADDR_NM".equals(columnName) ||
                "UPDATE_DTS".equals(columnName)) {
                continue; // 감사 컬럼은 건너뛰기
            }

            // <if> 태그로 각 일반 컬럼을 감싸줍니다.
            setBuilder.append(INDENT);
            if(useIfUpadate){
                String dataType = col.getData_type();
                // 데이터 타입이 문자열(VARCHAR, CHAR) 계열인지 확인
                if (dataType.startsWith("VARCHAR") || dataType.startsWith("CHAR") || dataType.startsWith("NVARCHAR")) {
                    // 문자열 타입일 경우: null 체크 AND 빈 문자열 체크
                    setBuilder.append("<if test=\"").append(paramName).append(" != null and ").append(paramName).append(" != ''\">");
                } else {
                    // NUMBER, DATE 등 그 외 모든 타입일 경우: null 체크만
                    setBuilder.append("<if test=\"").append(paramName).append(" != null\">");
                }
            }
            setBuilder.append(columnNameFormatted).append("= #{").append(paramName).append("},");
            if(useIfUpadate){
                setBuilder.append(" </if>");
            }
            setBuilder.append("\n");
        }
        setBuilder.append(INDENT).append(String.format("%-" + 20 + "s", "UPDATE_ID"))
                  .append("= #{P_UPDATE_ID},\n");
        setBuilder.append(INDENT).append(String.format("%-" + 20 + "s", "UPDATE_DTS"))
                  .append("= FN_TODATE(#{P_CM_SYSDATE}),\n");

        // <set> 태그 닫기
        setBuilder.append(INDENT).append("</set>\n");

        // --- WHERE 절 포매팅 ---
        whereBuilder.append(INDENT).append("<where>\n");
        for (ColumnInfo pkCol : pkColumns) {
            String columnName = pkCol.getColumn_name();
            String columnNameFormatted = String.format("%-" + 20 + "s", columnName);
            String finalParamName;

            // paramStyle 값에 따라 파라미터 형식을 분기 처리합니다.
            if (isParameterTypeModel) {
                // Model 스타일 (기본값): #{소문자컬럼명}
                finalParamName = lowerCaseSnakeCase(columnName);
            } else {
                // HashMap 스타일: #{P_대문자컬럼명}
                finalParamName = "P_" + columnName;
            }

            if(useIfWhere){
                whereBuilder.append(INDENT);
                String dataType = pkCol.getData_type();
                // 데이터 타입이 문자열(VARCHAR, CHAR) 계열인지 확인
                if (dataType.startsWith("VARCHAR") || dataType.startsWith("CHAR") || dataType.startsWith("NVARCHAR")) {
                    // 문자열 타입일 경우: null 체크 AND 빈 문자열 체크
                    whereBuilder.append("<if test=\"").append(finalParamName).append(" != null and ").append(finalParamName).append(" != ''\">\n");
                } else {
                    // NUMBER, DATE 등 그 외 모든 타입일 경우: null 체크만
                    whereBuilder.append("<if test=\"").append(finalParamName).append(" != null\">\n");
                }
            }
            whereBuilder.append(INDENT);
            whereBuilder.append("AND ").append(columnNameFormatted).append("= #{").append(finalParamName).append("}\n");
            if(useIfWhere){
                whereBuilder.append(INDENT);
                whereBuilder.append("</if>\n");
            }
        }
        whereBuilder.append(INDENT).append("</where>\n");

        String parameterType = isParameterTypeModel ? "Model" : "HashMap";

        sqlBuilder.append("\n    \n");
        sqlBuilder.append("    <update id=\"update")
                  .append("\" parameterType=\"").append(parameterType).append("\">\n");
        sqlBuilder.append("        \n");
        sqlBuilder.append("        UPDATE ").append(tableName).append("\n");
        sqlBuilder.append("        \n");
        sqlBuilder.append(setBuilder);
        sqlBuilder.append("        \n");
        sqlBuilder.append(whereBuilder);
        sqlBuilder.append("        \n");
        sqlBuilder.append("    </update>");

        return sqlBuilder.toString();
    }

    /**
     * MyBatis DELETE 구문을 생성합니다. (PK 컬럼을 WHERE 절에 사용)
     * @param tableName 테이블 이름
     * @param columns 테이블 컬럼 정보 리스트
     * @return 생성된 DELETE XML 구문
     */
    public static String generateDeleteStatement(String tableName, List<ColumnInfo> columns, boolean useIfWhere, boolean isParameterTypeModel) {
        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder whereBuilder = new StringBuilder();

        List<ColumnInfo> pkColumns = columns.stream()
                .filter(col -> "Y".equals(col.getPrimary_key()))
                .toList();

        // --- WHERE 절 포매팅 ---
        whereBuilder.append(INDENT).append("<where>\n");
        for (ColumnInfo pkCol : pkColumns) {
            String columnName = pkCol.getColumn_name();
            String columnNameFormatted = String.format("%-" + 20 + "s", columnName);
            String finalParamName;

            // paramStyle 값에 따라 파라미터 형식을 분기 처리합니다.
            if (isParameterTypeModel) {
                // Model 스타일 (기본값): #{소문자컬럼명}
                finalParamName = lowerCaseSnakeCase(columnName);
            } else {
                // HashMap 스타일: #{P_대문자컬럼명}
                finalParamName = "P_" + columnName;
            }

            if(useIfWhere){
                whereBuilder.append(INDENT);

                String dataType = pkCol.getData_type();
                // 데이터 타입이 문자열(VARCHAR, CHAR) 계열인지 확인
                if (dataType.startsWith("VARCHAR") || dataType.startsWith("CHAR") || dataType.startsWith("NVARCHAR")) {
                    // 문자열 타입일 경우: null 체크 AND 빈 문자열 체크
                    whereBuilder.append("<if test=\"").append(finalParamName).append(" != null and ").append(finalParamName).append(" != ''\">\n");
                } else {
                    // NUMBER, DATE 등 그 외 모든 타입일 경우: null 체크만
                    whereBuilder.append("<if test=\"").append(finalParamName).append(" != null\">\n");
                }
            }
            whereBuilder.append(INDENT);
            whereBuilder.append("AND ").append(columnNameFormatted).append("= #{").append(finalParamName).append("}\n");
            if(useIfWhere){
                whereBuilder.append(INDENT);
                whereBuilder.append("</if>\n");
            }
        }
        whereBuilder.append(INDENT).append("</where>\n");

        String parameterType = isParameterTypeModel ? "Model" : "HashMap";
        sqlBuilder.append("\n    \n");
        sqlBuilder.append("    <delete id=\"delete")
                  .append("\" parameterType=\"").append(parameterType).append("\">\n");
        sqlBuilder.append("        \n");
        sqlBuilder.append("        DELETE FROM ").append(tableName).append("\n");
        sqlBuilder.append(whereBuilder);
        sqlBuilder.append("        \n");
        sqlBuilder.append("    </delete>");

        return sqlBuilder.toString();
    }


    // 컬럼명을 소문자 스네이크 케이스로 변환 (예: COMPANY_CD -> company_cd)
    private static String lowerCaseSnakeCase(String colName) {
        return colName.toLowerCase();
    }

    // 테이블 이름을 CamelCase로 변환 (예: ME_ITEMSERCHK_INFO_X20400 -> MeItemserchkInfoX20400)
    private static String convertToCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return "";
        }
        StringBuilder camelCaseBuilder = new StringBuilder();
        boolean capitalizeNext = true; // 첫 글자는 대문자로 시작
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    camelCaseBuilder.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    camelCaseBuilder.append(Character.toLowerCase(c));
                }
            }
        }
        return camelCaseBuilder.toString();
    }

    /**
     * 리스트의 항목들을 지정된 colsPerLine에 맞춰 줄바꿈하고 고정된 패딩으로 정렬합니다.
     * col1,    col2,    col3 형태로 출력됩니다.
     * @param items 포매팅할 문자열 리스트 (컬럼명 또는 바인드 변수)
     * @param colsPerLine 한 줄에 표시할 항목 수
     * @param delimiter 항목 구분자 (예: ",")
     * @param padding 각 항목 뒤에 추가할 공백 수
     * @return 포매팅된 문자열
     */
    private static String simplifiedFormatLineByLine(List<String> items, int colsPerLine, String delimiter, int padding) {
        StringBuilder formattedBuilder = new StringBuilder();
        String paddingStr = " ".repeat(padding); // 지정된 패딩 길이만큼 공백 문자열 생성

        for (int i = 0; i < items.size(); i++) {
            if (i % colsPerLine == 0) { // 새 줄 시작 시 들여쓰기
                if (i > 0) {
                    formattedBuilder.append("\n");
                }
                formattedBuilder.append(INDENT);
            }

            formattedBuilder.append(items.get(i));

            if (i < items.size() - 1) { // 마지막 항목이 아니면 구분자 및 패딩 추가
                formattedBuilder.append(delimiter);
                if ((i + 1) % colsPerLine != 0) { // 한 줄의 마지막 항목이 아니면 패딩 추가
                    formattedBuilder.append(paddingStr);
                }
            }
        }
        return formattedBuilder.toString();
    }
}
