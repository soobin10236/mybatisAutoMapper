module org.dev.mybatisautomapper {
    //requires <모듈이름>: 해당 모듈이 런타임에 반드시 필요로 하는 모듈을 선언
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;

    requires java.sql; // JDBC를 사용한다면 필요
    requires com.google.gson; // Gson 라이브러리가 모듈이라면
    requires okhttp3;       // OkHttp 라이브러리가 모듈이라면
    requires org.mybatis;   // Mybatis 라이브러리가 모듈이라면 (모듈화 되어있다면)
    requires org.apache.logging.log4j; // Log4j가 모듈이라면
    requires org.apache.logging.log4j.core;
    requires org.fxmisc.richtext; // Log4j Core 모듈

    //opens <패키지>: 해당 패키지를 “완전 리플렉션”에 개방
    //런타임에 리플렉션으로 모든 멤버(클래스·필드·메서드)에 접근할 수 있게 함.
    opens org.dev.mybatisautomapper.view to javafx.fxml;    // FXML 파일이 컨트롤러에 접근할 수 있도록
    opens org.dev.mybatisautomapper.viewmodel to javafx.fxml;
    opens org.dev.mybatisautomapper.util to com.google.gson;

    //exports <패키지>: 이 모듈이 외부에 공개(public API)로 내놓는 패키지
    exports org.dev.mybatisautomapper.view;
    exports org.dev.mybatisautomapper.model;
}