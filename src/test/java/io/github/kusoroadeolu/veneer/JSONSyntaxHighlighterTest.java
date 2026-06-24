package io.github.kusoroadeolu.veneer;

import io.github.kusoroadeolu.clique.parser.MarkupParser;
import io.github.kusoroadeolu.veneer.theme.SyntaxThemes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JSONSyntaxHighlighterTest {
    private SyntaxHighlighter highlighter;

    @BeforeEach
    void setUp() {
        highlighter = new JSONSyntaxHighlighter(false);
    }

    @Test
    void highlight_objectKey_shouldBeStyledAsConstant() {
        String snippet = """
            {"name": "Alice"}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
    }

    @Test
    void highlight_stringValue_shouldBeStyledAsStringLiteral() {
        String snippet = """
            {"name": "Alice"}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }

    @Test
    void highlight_numberValue_shouldBeStyled() {
        String snippet = """
            {"age": 30}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.numberLiteral().ansiSequence()));
    }

    @Test
    void highlight_floatValue_shouldBeStyled() {
        String snippet = """
            {"score": 9.81}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.numberLiteral().ansiSequence()));
    }

    @Test
    void highlight_negativeNumber_shouldBeStyled() {
        String snippet = """
            {"temp": -42}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.numberLiteral().ansiSequence()));
    }

    @Test
    void highlight_trueKeyword_shouldBeStyled() {
        String snippet = """
            {"active": true}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.keyword().ansiSequence()));
    }

    @Test
    void highlight_falseKeyword_shouldBeStyled() {
        String snippet = """
            {"active": false}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.keyword().ansiSequence()));
    }

    @Test
    void highlight_nullKeyword_shouldBeStyled() {
        String snippet = """
            {"data": null}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.keyword().ansiSequence()));
    }

    @Test
    void highlight_nestedObject_keysShouldBeStyledAsConstants() {
        String snippet = """
            {"user": {"name": "Bob", "age": 25}}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
    }

    @Test
    void highlight_arrayOfStrings_shouldStyleValues() {
        String snippet = """
            {"tags": ["java", "antlr", "parsing"]}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }

    @Test
    void highlight_arrayOfNumbers_shouldStyleValues() {
        String snippet = """
            {"scores": [1, 2, 3]}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.numberLiteral().ansiSequence()));
    }

    @Test
    void highlight_emptyObject_shouldNotThrow() {
        String snippet = "{}";
        assertDoesNotThrow(() -> highlighter.highlight(snippet));
    }

    @Test
    void highlight_emptyArray_shouldNotThrow() {
        String snippet = """
            {"items": []}
            """;
        assertDoesNotThrow(() -> highlighter.highlight(snippet));
    }

    @Test
    void highlight_nullOrBlank_shouldReturnEmpty() {
        String s = null;
        assertTrue(highlighter.highlight(s).isEmpty());
        assertTrue(highlighter.highlight("").isEmpty());
        assertTrue(highlighter.highlight("   ").isEmpty());
    }

    @Test
    void highlight_keyAndValueAreDifferentStyles() {
        String snippet = """
            {"language": "java"}
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }

    @Test
    void highlight_withLineNumbers_shouldFormatCorrectly() {
        String snippet = """
            {
              "a": 1,
              "b": 2,
              "c": 3
            }
            """;
        var lineHighlighter = new JSONSyntaxHighlighter();
        String styled = MarkupParser.DEFAULT.getOriginalString(lineHighlighter.highlight(snippet));
        List<String> lines = styled.lines().toList();
        assertTrue(lines.getFirst().contains("1"));
        assertTrue(lines.get(1).contains("2"));
        assertTrue(lines.get(2).contains("3"));
    }
}