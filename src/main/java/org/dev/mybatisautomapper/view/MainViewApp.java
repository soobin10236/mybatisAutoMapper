package org.dev.mybatisautomapper.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainViewApp extends Application {
    private static final Logger logger = LogManager.getLogger(MainViewApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/dev/mybatisautomapper/MainView.fxml")
            );

            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 800);

            //css 설정
            URL cssUrl = getClass().getResource("/style.css");

            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                logger.warn("style.css 파일을 찾을 수 없어 기본 스타일로 실행됩니다.");
            }
            
            //Icon 가져오기
            try (InputStream iconStream = getClass().getResourceAsStream("/icon.png")) {
                // 1. iconStream이 null이 아닌지 확인합니다.
                if (iconStream != null) {
                    // 2. null이 아닐 경우에만 Image 객체를 생성하고 아이콘을 설정합니다.
                    Image appIcon = new Image(iconStream);
                    primaryStage.getIcons().add(appIcon);
                } else {
                    // (선택사항) 개발자를 위해 아이콘 로드 실패를 콘솔에 알림
                    System.out.println("메인 아이콘 파일을 찾을 수 없습니다.");
                }
            } catch (Exception e) {
                logger.error("애플리케이션 시작 중 오류가 발생했습니다.", e);
            }

            primaryStage.setTitle("Mybatis Auto Mapper");
            primaryStage.setScene(scene);

            primaryStage.show();
        } catch (Exception e) {
            // 예외 발생 시 이곳에서 처리합니다.
            logger.error("애플리케이션 시작 중 오류가 발생했습니다.", e);

            // 사용자에게 보여줄 알림창 생성
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("오류");
            alert.setHeaderText("프로그램 시작 중 오류가 발생했습니다.");
            alert.setContentText("설정 파일(config.json)을 찾을 수 없거나 파일이 손상되었습니다.\n프로그램을 종료합니다.");
            alert.getButtonTypes().add(ButtonType.OK);

            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            try (InputStream iconStream = getClass().getResourceAsStream("/icon.png")) {
                // 1. iconStream이 null이 아닌지 확인합니다.
                if (iconStream != null) {
                    // 2. null이 아닐 경우에만 Image 객체를 생성하고 아이콘을 설정합니다.
                    Image appIcon = new Image(iconStream);
                    alertStage.getIcons().add(appIcon);
                } else {
                    // (선택사항) 개발자를 위해 아이콘 로드 실패를 콘솔에 알림
                    System.out.println("메인 아이콘 파일을 찾을 수 없습니다.");
                }
            } catch (Exception ec) {
                logger.error("애플리케이션 시작 중 오류가 발생했습니다.", ec);
            }

            // 사용자가 확인 버튼을 누를 때까지 대기
            alert.showAndWait();
        }
    }
    public static void main(String[] args) { launch(); }
}