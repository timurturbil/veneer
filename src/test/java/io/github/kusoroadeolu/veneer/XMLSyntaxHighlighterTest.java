package io.github.kusoroadeolu.veneer;

import io.github.kusoroadeolu.clique.parser.MarkupParser;
import io.github.kusoroadeolu.veneer.theme.SyntaxThemes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XMLSyntaxHighlighterTest {
    private SyntaxHighlighter highlighter;

    @BeforeEach
    void setUp() {
        highlighter = new XMLSyntaxHighlighter(false);
    }

    @Test
    void highlight_comment_shouldBeStyled() {
        String snippet = "<!-- this is a comment -->";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.comment().ansiSequence()));
    }

    @Test
    void highlight_multiLineComment_shouldBeStyled() {
        String snippet = """
            <!-- this is
                 a multi line
                 comment -->
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.comment().ansiSequence()));
    }

    @Test
    void highlight_processingInstruction_shouldBeStyled() {
        String snippet = "<?xml-stylesheet type=\"text/css\" href=\"style.css\"?>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.comment().ansiSequence()));
    }

    @Test
    void highlight_xmlDeclaration_shouldBeStyled() {
        String snippet = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.keyword().ansiSequence()));
    }

    @Test
    void highlight_attributeValue_shouldBeStyled() {
        String snippet = "<product id=\"001\">";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }

    @Test
    void highlight_attributeName_shouldBeStyled() {
        String snippet = "<product id=\"001\">";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
    }

    @Test
    void highlight_tagName_shouldBeStyled() {
        String snippet = "<product></product>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.types().ansiSequence()));
    }

    @Test
    void highlight_entityRef_shouldBeStyled() {
        String snippet = "<name>Keyboard &amp; Mouse</name>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
    }

    @Test
    void highlight_charRef_decimal_shouldBeStyled() {
        String snippet = "<note>caf&#233; au lait</note>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
    }

    @Test
    void highlight_charRef_hex_shouldBeStyled() {
        String snippet = "<note>&#x24;10 off</note>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
    }

    @Test
    void highlight_cdata_shouldBeStyled() {
        String snippet = "<desc><![CDATA[<b>bold</b> & stuff]]></desc>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }

    @Test
    void highlight_multiLineCdata_shouldBeStyled() {
        String snippet = """
            <desc><![CDATA[
                <b>bold</b> & stuff
                more content here
            ]]></desc>
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }

    @Test
    void highlight_dtd_shouldNotDisappear() {
        String snippet = """
            <!DOCTYPE catalog SYSTEM "catalog.dtd">
            <catalog></catalog>
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(MarkupParser.DEFAULT.getOriginalString(styled).contains("<!DOCTYPE"));
    }

    @Test
    void highlight_selfClosingTag_shouldBeStyled() {
        String snippet = "<br/>";
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.types().ansiSequence()));
    }

    @Test
    void highlight_nullOrBlank_shouldReturnEmpty() {
        assertTrue(highlighter.highlight((String) null).isEmpty());
        assertTrue(highlighter.highlight("").isEmpty());
        assertTrue(highlighter.highlight("   ").isEmpty());
    }

    @Test
    void highlight_withLineNumbers_shouldFormatCorrectly() {
        String snippet = """
            <root>
                <child>text</child>
                <child>text</child>
            </root>
            """;
        var lineHighlighter = new XMLSyntaxHighlighter();
        String styled = MarkupParser.DEFAULT.getOriginalString(lineHighlighter.highlight(snippet));
        List<String> lines = styled.lines().toList();
        assertTrue(lines.getFirst().contains("1"));
        assertTrue(lines.get(1).contains("2"));
        assertTrue(lines.get(2).contains("3"));
        assertTrue(lines.get(3).contains("4"));
    }

    @Test
    void highlight_multiLineCdata_withLineNumbers_shouldFormatCorrectly() {
        String snippet = """
            <desc><![CDATA[
                line two
                line three
            ]]></desc>
            <other/>
            """;
        var lineHighlighter = new XMLSyntaxHighlighter();
        String styled = MarkupParser.DEFAULT.getOriginalString(lineHighlighter.highlight(snippet));
        List<String> lines = styled.lines().toList();
        assertTrue(lines.getFirst().contains("1"));
        assertTrue(lines.get(1).contains("2"));
        assertTrue(lines.get(2).contains("3"));
        assertTrue(lines.get(3).contains("4"));
        assertTrue(lineHighlighter.highlight(snippet).contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }

    @Test
    void highlight_multiLineComment_withLineNumbers_shouldFormatCorrectly() {
        String snippet = """
            <!-- line one
                 line two
                 line three -->
            <root/>
            """;
        var lineHighlighter = new XMLSyntaxHighlighter();
        String styled = MarkupParser.DEFAULT.getOriginalString(lineHighlighter.highlight(snippet));
        List<String> lines = styled.lines().toList();
        assertTrue(lines.getFirst().contains("1"));
        assertTrue(lines.get(1).contains("2"));
        assertTrue(lines.get(2).contains("3"));
        assertTrue(lines.get(3).contains("4"));
        assertTrue(lineHighlighter.highlight(snippet).contains(SyntaxThemes.DEFAULT.comment().ansiSequence()));

    }

    @Test
    void highlight_nestedElements_tagNamesShouldBeStyled() {
        String snippet = """
            <catalog>
                <product id="001">
                    <name>Widget</name>
                </product>
            </catalog>
            """;
        String styled = highlighter.highlight(snippet);
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.types().ansiSequence()));
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.constants().ansiSequence()));
        assertTrue(styled.contains(SyntaxThemes.DEFAULT.stringLiteral().ansiSequence()));
    }
}