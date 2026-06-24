package io.github.kusoroadeolu.veneer;

import io.github.kusoroadeolu.clique.parser.MarkupParser;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.kusoroadeolu.veneer.BibTeXLexer;

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

    @Test
    void quotedStringWithBraceEscapedQuotes_shouldPreserveContent() {
        // {"} inside a quoted string is a brace-escaped quote — valid BibTeX
        String snippet = """
            @block_4{with_quotation_marks,
              title = {With "Quotation" Marks},
              author = "Marky {"}The Beasty{"} Quoto",
              year = "2016"
            }
        """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        // The title field should be tokenized as BRACE_STRING and contain "Quotation"
        assertTrue(tokenStream.getTokens().stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.BRACE_STRING
                        && t.getText().contains("Quotation")));

        // The author field should be tokenized as DQUOTE_STRING
        assertTrue(tokenStream.getTokens().stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.DQUOTE_STRING
                        && t.getText().contains("Marky")));

        // The year field should be parsed correctly — "2016" should be a DQUOTE_STRING
        assertTrue(tokenStream.getTokens().stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.DQUOTE_STRING
                        && t.getText().equals("\"2016\"")));

        // The year field should still be visible — the following field should be lexed correctly
        assertTrue(tokenStream.getTokens().stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.NAME_TOKEN
                        && t.getText().equals("year")));
    }

    @Test
    void stringConcatenation_shouldTokenizeCorrectly() {
        String snippet = """
            @String { kopp   = "Kopp, Oliver" }
            @String { kubovy = "Kubovy, Jan" }
            @String { et     = " and " }
        
            @Misc{m1,
              author = kopp # et # kubovy,
            }
        
            @Misc{m2,
              author = kopp # " and " # kubovy,
            }
        """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        List<Token> tokens = tokenStream.getTokens();

        // All @String keywords should be recognized
        assertEquals(3, tokens.stream()
                .filter(t -> t.getType() == BibTeXLexer.AT_STRING)
                .count());

        // The # operator should be tokenized as CONCAT
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == BibTeXLexer.CONCAT));

        // kopp, et, and kubovy should be tokenized as NAME_TOKENs (macro references)
        assertTrue(tokens.stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.NAME_TOKEN && t.getText().equals("kopp")));
        assertTrue(tokens.stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.NAME_TOKEN && t.getText().equals("et")));
        assertTrue(tokens.stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.NAME_TOKEN && t.getText().equals("kubovy")));
    }

    @Test
    void bareMonthMacro_shouldBeNameToken() {
        String snippet = """
            @Misc{m3,
              month = may,
            }
        """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        // Bare month macro should be tokenized as NAME_TOKEN, not as a string
        assertTrue(tokenStream.getTokens().stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.NAME_TOKEN && t.getText().equals("may")));

        assertFalse(tokenStream.getTokens().stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.BRACE_STRING && t.getText().equals("{may}")));
    }

    @Test
    void bracedMonthMacro_shouldBeBraceString() {
        String snippet = """
            @Misc{m4,
              month = {may},
            }
        """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        // {may} should be tokenized as BRACE_STRING
        assertTrue(tokenStream.getTokens().stream()
                .anyMatch(t -> t.getType() == BibTeXLexer.BRACE_STRING && t.getText().equals("{may}")));
    }

    @Test
    void typedStrings_shouldAllBeAtString() {
        String snippet = """
            @String { aKopp    = "Kopp, Oliver" }
            @String { iMIT     = "{Massachusetts Institute of Technology ({MIT})}" }
            @String { pMIT     = "{Massachusetts Institute of Technology ({MIT}) press}" }
            @String { anct     = "Anecdote" }
            @String { lBigMac  = "Big Mac" }
        """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        assertEquals(5, tokenStream.getTokens().stream()
                .filter(t -> t.getType() == BibTeXLexer.AT_STRING)
                .count());
    }

    @Test
    void mixedConcatenationWithStringsAndMacros_shouldTokenizeCorrectly() {
        String snippet = """
            @String { aStallman = "Stallman, Richard" }
            @String { aKahle    = "Kahle, Brewster" }
            @String { iMIT      = "{Massachusetts Institute of Technology ({MIT})}" }
            @String { pMIT      = "{Massachusetts Institute of Technology ({MIT}) press}" }
            @String { eg        = "for example" }
            @String { et        = " and " }
        
            @Misc{gnuproject,
              title       = "The GNU Project",
              author      = aStallman # et # aKahle,
              institution = iMIT,
              publisher   = pMIT,
              note        = "Just " # eg,
            }
        """;

        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(snippet));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        List<Token> tokens = tokenStream.getTokens();

        // There should be 6 @String definitions
        assertEquals(6, tokens.stream()
                .filter(t -> t.getType() == BibTeXLexer.AT_STRING)
                .count());

        // The @Misc entry should be recognized
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == BibTeXLexer.AT_ENTRY));

        // # concatenation operators must be existed
        assertTrue(tokens.stream().filter(t -> t.getType() == BibTeXLexer.CONCAT).count() >= 3);

        // Macro references should be tokenized as NAME_TOKENs
        assertTrue(tokens.stream().anyMatch(t ->
                t.getType() == BibTeXLexer.NAME_TOKEN && t.getText().equals("aStallman")));
        assertTrue(tokens.stream().anyMatch(t ->
                t.getType() == BibTeXLexer.NAME_TOKEN && t.getText().equals("iMIT")));

        // Field names should be recognized correctly
        Token institutionToken = tokens.stream()
                .filter(t -> t.getType() == BibTeXLexer.NAME_TOKEN && t.getText().equals("institution"))
                .findFirst().orElseThrow();
        assertTrue(highlighter.isFieldName(institutionToken, tokens, tokens.indexOf(institutionToken)));
    }
}