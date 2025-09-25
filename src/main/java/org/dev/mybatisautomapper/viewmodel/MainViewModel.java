package org.dev.mybatisautomapper.viewmodel;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.dev.mybatisautomapper.model.ColumnInfo;
import org.dev.mybatisautomapper.service.AiService;
import org.dev.mybatisautomapper.service.TableInfoService;
import org.dev.mybatisautomapper.util.MybatisMapperGenerator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class MainViewModel {
    public StringProperty tableName = new SimpleStringProperty();
    public StringProperty status = new SimpleStringProperty("Ready");
    public StringProperty logOutput = new SimpleStringProperty("");
    public BooleanProperty useIfUpdate = new SimpleBooleanProperty(true); // 기본값 true
    public BooleanProperty useIfWhere = new SimpleBooleanProperty(false); // 기본값 false
    // true이면 Model, false이면 HashMap
    public BooleanProperty isParameterTypeModel = new SimpleBooleanProperty(true); // 기본값 true

    private final TableInfoService tableInfoService;
    private final AiService aiService;

    public MainViewModel(TableInfoService tableInfoService, AiService aiService) {
        this.tableInfoService = tableInfoService;
        this.aiService = aiService;
    }


    /**
     * DB 연결 테스트 버튼 이벤트 핸들러
     */
    public void onTestConnection() {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateMessage("DB 연결 테스트 중...");
                return tableInfoService.checkDbConnection();
            }

            @Override
            protected void succeeded() {
                // UI 업데이트는 Platform.runLater 내부에서만
                if (getValue()) { // Task의 결과값 (boolean)
                    updateMessage("DB 연결 성공!");
                    logOutput.set("데이터베이스 연결에 성공했습니다."); // 성공 메시지도 로그에
                } else {
                    updateMessage("DB 연결 실패! 설정 확인 필요.");
                    logOutput.set("데이터베이스 연결에 실패했습니다. 설정(config.json)을 확인하거나 방화벽, 서버 상태를 점검해주세요."); // 실패 메시지도 로그에
                }
            }

            @Override
            protected void failed() {
                // Task에서 예외 발생 시 (예: MyBatisUtil.testConnection()에서 RuntimeException 발생 시)
                Throwable e = getException();
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();

                updateMessage("DB 연결 테스트 중 예외 발생: " + e.getMessage());
                logOutput.set("DB 연결 테스트 중 예외 발생:\n" + exceptionAsString); // <-- 여기에 스택 트레이스 전체를 기록
            }

            @Override
            protected void cancelled() {
                updateMessage("DB 연결 테스트 취소됨.");
                logOutput.set("DB 연결 테스트가 취소되었습니다.");
            }
        };

        status.bind(task.messageProperty()); // Task의 메시지 속성과 바인딩
        logOutput.set("");
        new Thread(task).start();
    }

    /**
     * 실제 쿼리 호출 (컬럼 정보 조회) 버튼 이벤트 핸들러
     */
    public void onFetchColumns() {
        String currentTableName = tableName.get();
        boolean currentUseIfUpdate = useIfUpdate.get();
        boolean currentUseIfWhere = useIfWhere.get();
        boolean currentIsParameterTypeModel = isParameterTypeModel.get();
        if (currentTableName == null || currentTableName.trim().isEmpty()) {
            status.set("테이블 이름을 입력해주세요.");
            logOutput.set("테이블 이름을 입력해야 컬럼을 조회하고 매퍼를 생성할 수 있습니다.");
            return;
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // --- 1. 백그라운드 작업만 수행 ---
                updateMessage("테이블 메타데이터 조회 중...");

                // TableInfoService를 통해 컬럼 정보 조회
                List<ColumnInfo> cols = tableInfoService.fetchColumns(currentTableName);

                if (cols.isEmpty()) {//비어있으면 테이블 명이 잘못된 경우로 볼 수 있음.
                    throw new IllegalStateException("테이블 '" + currentTableName + "'에 대한 컬럼 정보를 찾을 수 없습니다.");
                }
                updateMessage("매퍼 생성 완료.");

                String generatedMapper = "";
                String modelName = convertToModelName(currentTableName); // 테이블명 기반 모델명 유추
                String selectStmt = MybatisMapperGenerator.generateSelectStatement(currentTableName, cols, currentUseIfWhere, currentIsParameterTypeModel);
                String insertStmt = MybatisMapperGenerator.generateInsertStatement(currentTableName, cols);
                String updateStmt = MybatisMapperGenerator.generateUpdateStatement(currentTableName, cols, currentUseIfUpdate, currentUseIfWhere, currentIsParameterTypeModel);
                String deleteStmt = MybatisMapperGenerator.generateDeleteStatement(currentTableName, cols, currentUseIfWhere, currentIsParameterTypeModel);

                // 생성된 구문을 하나의 문자열로 합쳐서 logOutput에 담습니다.
                // ListView에는 적합하지 않으므로, TextArea에 보여주는 것이 좋습니다.
                generatedMapper = "\n" + selectStmt + "\n" +
                        "\n" + insertStmt + "\n" +
                        "\n" + updateStmt + "\n" +
                        "\n" + deleteStmt;

                return generatedMapper;    // Task 결과로 컬럼 리스트 반환 (선택 사항)
            }

            @Override
            protected void succeeded() {
                String mapperStr = getValue(); // Task의 결과값 (List<ColumnInfo>)
                if (mapperStr != null) {
                    //화면에 표시
                    updateMessage("mapper 생성 완료!");
                    logOutput.set(mapperStr);
                } else {
                    updateMessage("mapper 생성 실패..");
                    logOutput.set("테이블 '" + currentTableName + "'에 대한 컬럼 정보를 찾을 수 없습니다. 테이블 이름이 정확한지 확인해주세요.");
                }
            }

            @Override
            protected void failed() {
                Throwable e = getException();
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();

                updateMessage("쿼리 실행 실패..");
                logOutput.set("쿼리 실행 중 예외 발생:\n" + exceptionAsString); // <-- 여기에 스택 트레이스 전체를 기록
            }

            @Override
            protected void cancelled() {
                updateMessage("쿼리 실행 취소됨.");
                logOutput.set("쿼리 실행이 취소되었습니다.");
            }
        };

        status.bind(task.messageProperty());
        logOutput.set("");
        new Thread(task).start();
    }

    public void OnCopyToClipboard() {
        String copyContent = logOutput.get();
        status.unbind();
        if (copyContent != null && !copyContent.isEmpty()) {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(copyContent);
            Clipboard.getSystemClipboard().setContent(clipboardContent);

            // Task가 없으므로 status.bind 대신 직접 status.set 사용
            status.set("클립보드에 복사되었습니다.");
        } else {
            status.set("복사할 내용이 없습니다.");
        }

    }

    // 테이블 이름을 모델 클래스 이름으로 변환 (예: ME_ITEMSERCHK_INFO_X20400 -> MeItemserchkInfoX20400)
    private String convertToModelName(String tableName) {
        StringBuilder modelNameBuilder = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : tableName.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    modelNameBuilder.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    modelNameBuilder.append(Character.toLowerCase(c));
                }
            }
        }
        return modelNameBuilder.toString();
    }

    // AI 서비스 호출 로직은 onFetchColumns()와는 별개로 필요하다면 다른 메서드로 분리하는 것이 좋습니다.
    // 예를 들어, onGenerateAIResponse() 같은 메서드를 만들어서 onFetchColumns()로 컬럼 정보를 가져온 후
    // 그 정보를 바탕으로 AI 호출을 진행하도록 할 수 있습니다.
    // 현재는 주석 처리된 AI 관련 코드를 그대로 유지합니다.

    private String buildPrompt(String table, List<ColumnInfo> cols) {
        // 기존 buildPrompt 로직은 유지
        StringBuilder sb = new StringBuilder();
        sb.append("다음 테이블의 컬럼 정보를 기반으로 MyBatis XML 매퍼를 생성해줘.\n");
        sb.append("테이블명: ").append(table).append("\n컬럼:\n");
        for (ColumnInfo c : cols) {
            sb.append("- ").append(c.getColumn_name())
                    .append(" (").append(c.getData_type()).append(")\n");
        }
        sb.append("\n결과만 XML 형태로 출력해줘.");
        return sb.toString();
    }


}
