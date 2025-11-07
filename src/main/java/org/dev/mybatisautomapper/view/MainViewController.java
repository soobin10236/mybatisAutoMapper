package org.dev.mybatisautomapper.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import javafx.util.Duration;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.dev.mybatisautomapper.service.AiService;
import org.dev.mybatisautomapper.service.AiServiceFactory;
import org.dev.mybatisautomapper.service.TableInfoService;
import org.dev.mybatisautomapper.util.Config;
import org.dev.mybatisautomapper.util.ConfigLoader;
import org.dev.mybatisautomapper.util.SyntaxHighlighting;
import org.dev.mybatisautomapper.viewmodel.MainViewModel;
import org.fxmisc.richtext.CodeArea;

import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;


public class MainViewController {
    @FXML
    private TextField tableInput;
    // 기존 generateBtn 대신, 새로운 버튼 2개 추가
    @FXML
    private Button testConnectionBtn; // DB 연결 테스트 버튼
    @FXML
    private Button fetchColumnsBtn;   // 컬럼 조회 버튼
    @FXML
    private Label statusLabel;
    @FXML
    private Button copyToClipboardBtn; // 복사 버튼 필드 추가
    @FXML
    private CodeArea logOutputArea;
    @FXML
    private CheckBox useIfInUpdateChk;
    @FXML
    private CheckBox useIfInWhereChk;
    @FXML
    private ToggleGroup paramTypeGroup;
    @FXML
    private RadioButton paramModelRadio;
    @FXML
    private RadioButton paramHashMapRadio;
    @FXML
    private BorderPane rootPane;
    @FXML
    private ToggleButton themeToggleBtn;

    private MainViewModel vm;

    @FXML
    public void initialize() {
        // 1) 설정 로드
        String configPath = "config.json";
        Config cfg = ConfigLoader.load(configPath);

        // 2) 서비스 초기화
        //    - DatabaseService는 MyBatisUtil 내부에서 config.json을 참조하도록 구현되어 있어요.
        TableInfoService dbService = new TableInfoService();
        //    - AIService는 factory를 통해 provider(defaultProvider)에 맞춰 생성
        AiService aiService = AiServiceFactory.create(cfg.getAi());

        // 3) ViewModel 생성
        vm = new MainViewModel(dbService, aiService);
        vm.init(cfg, configPath);

        // 4) View ↔ ViewModel 바인딩

        //테이블명은 대문자로 포맷팅 + 자동완성
        /*UnaryOperator<TextFormatter.Change> upperCaseFilter = change -> {
            change.setText(change.getText().toUpperCase());
            return change;
        };

        TextFormatter<String> upperCaseFormatter = new TextFormatter<>(upperCaseFilter);
        tableInput.setTextFormatter(upperCaseFormatter);*/
        tableInput.textProperty().bindBidirectional(vm.tableName);
        // 자동완성 기능 바인딩

        // 1. 자동완성 제안 규칙(Callback)을 정의합니다.
        Callback<AutoCompletionBinding.ISuggestionRequest, Collection<String>> suggestionProvider = request -> {
            // 사용자가 입력한 텍스트를 가져와서 대문자로 변환
            String userInput = request.getUserText().toUpperCase();

            // 16,000개의 캐시된 테이블 목록(vm.cachedTableNames)에서
            // 대문자 입력값(userInput)으로 "시작하는(startsWith)" 항목만 필터링
            return vm.cachedTableNames.stream()
                    .filter(tableName -> tableName.startsWith(userInput))
                    .collect(Collectors.toList());
        };

        // 2. 자동완성 기능을 바인딩할 때, 위에서 만든 커스텀 제안 규칙(suggestionProvider)을 전달합니다.
        TextFields.bindAutoCompletion(tableInput, suggestionProvider);

        // 3. (선택 사항) 사용자가 'hr_'를 입력하면 자동으로 'HR_'로 변환되도록 리스너를 다시 추가합니다.
        //    (자동완성 팝업 자체는 대소문자를 구분하지 않지만, UI 일관성을 위해)
        tableInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toUpperCase())) {
                Platform.runLater(() -> {
                    int caretPos = tableInput.getCaretPosition();
                    // ViewModel의 값을 변경하면 bindBidirectional을 통해 TextField도 변경됨
                    vm.tableName.set(newVal.toUpperCase());
                    tableInput.positionCaret(caretPos);
                });
            }
        });

        // 앱 시작 시 자동으로 테이블 목록을 로드하도록 VM에 요청합니다.
         vm.onTestConnection();

        //테이블명 세팅 끝


        statusLabel.textProperty().bind(vm.status);
        vm.logOutput.addListener((obs, oldText, newText) -> {
            logOutputArea.replaceText(newText);
        });
        setupSyntaxHighlighting(); //SyntaxHighlighting

        useIfInUpdateChk.selectedProperty().bindBidirectional(vm.useIfUpdate);
        useIfInWhereChk.selectedProperty().bindBidirectional(vm.useIfWhere);

        // ToggleGroup의 선택된 토글이 변경될 때마다 ViewModel의 isParameterTypeModel 속성을 업데이트합니다.
        paramTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            // 새로 선택된 토글(newToggle)이 paramModelRadio와 같으면 true, 아니면 false를 설정합니다.
            vm.isParameterTypeModel.set(newToggle == paramModelRadio);
        });

        // ViewModel의 darkThemeEnabledProperty가 변경될 때마다 호출될 리스너 추가
        vm.darkThemeEnabledProperty().addListener((obs, oldVal, newVal) -> {
            updateTheme(newVal);
        });

        // 추가: 버튼의 선택 상태와 ViewModel의 상태를 양방향으로 바인딩
        themeToggleBtn.selectedProperty().bindBidirectional(vm.darkThemeEnabledProperty());
        updateTheme(vm.isDarkThemeEnabled());   //최초 실행 시 테마 적용


        // 5) 입력 필드 비어 있으면 '컬럼 조회' 버튼 비활성
        // 'Generate' 버튼이었던 generateBtn은 삭제하고, 새로운 fetchColumnsBtn에 바인딩
        fetchColumnsBtn.disableProperty().bind(vm.tableName.isEmpty());
        // 'DB 연결 테스트' 버튼은 테이블 이름과 무관하게 항상 활성 (또는 필요시 다르게 바인딩)
        testConnectionBtn.disableProperty().bind(vm.tableName.isEmpty().not()); // 예시: 테이블 이름이 비어있지 않을 때만 활성화 (필요시 제거)
        // copyToClipboardBtn은 초기 상태에서 비활성화할 필요 없이 항상 활성화
        // 또는 logOutputArea에 내용이 있을 때만 활성화하도록 설정 가능
        copyToClipboardBtn.disableProperty().bind(vm.logOutput.isEmpty());
    }

    /**
     * DB 연결 테스트 버튼 클릭 시 호출될 메서드
     */
    @FXML
    public void onTestConnection() {
        vm.onTestConnection();
    }

    /**
     * 컬럼 조회 버튼 클릭 시 호출될 메서드
     */
    @FXML
    public void onFetchColumns() {
        vm.onFetchColumns();
    }

    @FXML
    private void onCopyToClipboard() {
        vm.OnCopyToClipboard();
    }

    private void updateTheme(boolean isDark) {
        if (isDark) {
            rootPane.getStyleClass().add("dark-theme");
        } else {
            rootPane.getStyleClass().remove("dark-theme");
        }
    }

    //메서드
    private void setupSyntaxHighlighting() {
        // 이 부분은 복잡하므로 별도의 클래스로 분리하는 것이 좋습니다.
        // 우선은 간단한 XML 하이라이팅 로직을 여기에 바로 추가합니다.
        SyntaxHighlighting.apply(logOutputArea);
    }
}