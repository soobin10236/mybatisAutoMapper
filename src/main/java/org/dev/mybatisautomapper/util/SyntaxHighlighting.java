package org.dev.mybatisautomapper.util;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighting {
    // 정규식 패턴 정의
    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>``|--[^\n]*)"                                     // 주석
          + "|(?<TAGBRACKET></?|>)"                                      // 태그 괄호: <, </, >
          + "|(?<TAGNAME>(?<=</?)\\w[\\w-]*)"                                    // 태그 이름
          + "|(?<ATTRIBUTE>\\w[\\w-]*)(?=\\s*=\\s*\")"                         // 속성 이름
          + "|(?<VALUE>\"[^\"]*\")"                                      // 속성 값
    );

    public static void apply(CodeArea codeArea) {
        // 텍스트가 변경될 때마다 하이라이팅을 다시 계산하여 적용
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            codeArea.setStyleSpans(0, computeHighlighting(newText));
        });
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        Collection<String> defaultStyle = Collections.singleton("default-text");

        while (matcher.find()) {
            // [수정] 각 그룹에 맞는 CSS 스타일 클래스를 지정
            String styleClass =
                    matcher.group("COMMENT") != null ? "xml-comment" :
                    matcher.group("TAGBRACKET") != null ? "xml-tag-bracket" :
                    matcher.group("TAGNAME") != null ? "xml-tag-name" :
                    matcher.group("ATTRIBUTE") != null ? "xml-attribute" :
                    matcher.group("VALUE") != null ? "xml-value" :
                    null; // null일 경우 없음

            if (styleClass != null) {
                spansBuilder.add(defaultStyle, matcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
                lastKwEnd = matcher.end();
            }
        }
        spansBuilder.add(defaultStyle, text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
