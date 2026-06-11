package io.github.kusoroadeolu.veneer;

import io.github.kusoroadeolu.clique.parser.MarkupParser;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BibTeXSyntaxHighlighterTest {
    private BibTeXSyntaxHighlighter highlighter;

    @BeforeEach
    void setUp() {
        highlighter = new BibTeXSyntaxHighlighter(false);
    }

    @Test
    void highlight_entryKeyword_shouldBeStyled() {
        String snippet = """
            @article{knuth1984,
              title = {Literate Programming}
            }
            """;
        String styled = highlighter.highlight(snippet);
        String plain = MarkupParser.DEFAULT.getOriginalString(styled);
        assertTrue(plain.contains("@article"));
        assertTrue(plain.contains("knuth1984"));
    }

    @Test
    void highlight_stringAndNumber_shouldBeStyled() {
        String snippet = """
            @string{jan = "January"}
            @article{sample,
              year = 2024,
              title = {A title}
            }
            """;
        String styled = highlighter.highlight(snippet);
        String plain = MarkupParser.DEFAULT.getOriginalString(styled);
        assertTrue(plain.contains("January"));
        assertTrue(plain.contains("2024"));
    }

    @Test
    void highlight_comment_shouldBeStyled() {
        String snippet = "% this is a comment";
        String styled = highlighter.highlight(snippet);
        String plain = MarkupParser.DEFAULT.getOriginalString(styled);
        assertTrue(plain.contains("this is a comment"));
    }

    @Test
    void commentEntry_shouldTreatInnerContentAsComment() {
        String snippet = """
            @comment{
              This is comment block.
              Everything inside should be ignored.
              author = {nobody},
            }
            """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        assertTrue(tokenStream.getTokens().stream().anyMatch(token -> token.getType() == BibTeXLexer.AT_COMMENT));
        assertFalse(tokenStream.getTokens().stream().anyMatch(token ->
                token.getType() == BibTeXLexer.NAME_TOKEN && token.getText().equals("author")));
        assertFalse(tokenStream.getTokens().stream().anyMatch(token ->
                token.getType() == BibTeXLexer.NAME_TOKEN && token.getText().equals("kimse")));
    }

    @Test
    void citeKeyWithNumericSuffix_shouldStaySingleToken() {
        String snippet = """
            @book{DUMMY:1,
              title = {The Book without Title}
            }
            """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        Token citeKey = tokenStream.getTokens().stream()
                .filter(token -> token.getType() == BibTeXLexer.NAME_TOKEN && token.getText().equals("DUMMY:1"))
                .findFirst()
                .orElseThrow();

        assertTrue(highlighter.isCiteKey(citeKey, tokenStream.getTokens(), tokenStream.getTokens().indexOf(citeKey)));
    }

    @Test
    void highlight_withLineNumbers_shouldFormatCorrectly() {
        String snippet = """
            @article{sample,
              title = {A title},
              note = {line one
                line two}
            }
            """;
        var lineHighlighter = new BibTeXSyntaxHighlighter();
        String styled = MarkupParser.DEFAULT.getOriginalString(lineHighlighter.highlight(snippet));
        List<String> lines = styled.lines().toList();
        assertTrue(lines.getFirst().contains("1"));
        assertTrue(lines.get(1).contains("2"));
        assertTrue(lines.get(2).contains("3"));
        assertTrue(lines.get(3).contains("4"));
    }

    @Test
    void citeKeyAndFieldNames_shouldBeClassifiedDifferently() {
        String snippet = """
            @inproceedings{DBLP:conf/pldi/PadhiSM16,
              author = {Saswat Padhi and Rahul Sharma},
              title = {Data-driven precondition inference}
            }
            """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        Token citeKey = tokenStream.getTokens().stream()
                .filter(token -> token.getType() == BibTeXLexer.NAME_TOKEN && token.getText().equals("DBLP:conf/pldi/PadhiSM16"))
                .findFirst()
                .orElseThrow();

        Token authorField = tokenStream.getTokens().stream()
                .filter(token -> token.getType() == BibTeXLexer.NAME_TOKEN && token.getText().equals("author"))
                .findFirst()
                .orElseThrow();

        Token titleField = tokenStream.getTokens().stream()
                .filter(token -> token.getType() == BibTeXLexer.NAME_TOKEN && token.getText().equals("title"))
                .findFirst()
                .orElseThrow();

        assertTrue(highlighter.isCiteKey(citeKey, tokenStream.getTokens(), tokenStream.getTokens().indexOf(citeKey)));
        assertTrue(highlighter.isFieldName(authorField, tokenStream.getTokens(), tokenStream.getTokens().indexOf(authorField)));
        assertTrue(highlighter.isFieldName(titleField, tokenStream.getTokens(), tokenStream.getTokens().indexOf(titleField)));
        assertFalse(highlighter.isFieldName(citeKey, tokenStream.getTokens(), tokenStream.getTokens().indexOf(citeKey)));
    }

    @Test
    void quotedString_shouldStopAtClosingQuote() {
        String snippet = "@Misc{test2, title1 = \"asdad\", title2 = \"{sdsd}\" }";
        String styled = highlighter.highlight(snippet);
        String plain = MarkupParser.DEFAULT.getOriginalString(styled);

        assertTrue(plain.contains("title1"));
        assertTrue(plain.contains("title2"));
        assertTrue(plain.contains("asdad"));
        assertTrue(plain.contains("{sdsd}"));
    }
}