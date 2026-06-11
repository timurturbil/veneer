package io.github.kusoroadeolu.veneer;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.style.StyleBuilder;
import io.github.kusoroadeolu.veneer.theme.SyntaxTheme;
import io.github.kusoroadeolu.veneer.utils.Constants;
import io.github.kusoroadeolu.veneer.utils.Utils;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.util.List;

import static io.github.kusoroadeolu.veneer.BibTeXLexer.*;
import static io.github.kusoroadeolu.veneer.utils.Utils.isNullOrBlank;
import static io.github.kusoroadeolu.veneer.utils.Utils.styleMultiLineToken;
import static org.antlr.v4.runtime.tree.xpath.XPathLexer.VOCABULARY;

public class BibTeXSyntaxHighlighter extends AbstractSyntaxHighlighter {

    public BibTeXSyntaxHighlighter() { super(); }
    public BibTeXSyntaxHighlighter(boolean showLineNumbers) { super(showLineNumbers); }
    public BibTeXSyntaxHighlighter(SyntaxTheme theme) { super(theme); }
    public BibTeXSyntaxHighlighter(SyntaxTheme theme, boolean showLineNumbers) { super(theme, showLineNumbers); }

    @Override
    public String highlight(String s) {
        if (isNullOrBlank(s)) return "";

        StyleBuilder sb = Clique.styleBuilder();
        BibTeXLexer lexer = new BibTeXLexer(CharStreams.fromString(s));
        BufferedTokenStream tokenStream = Utils.toTokenStream(lexer);

        if (showLineNumbers) applyWithLines(sb, tokenStream);
        else applyWithoutLines(sb, tokenStream);

        return sb.toString();
    }

    void applyWithLines(StyleBuilder sb, BufferedTokenStream tokenStream) {
        List<Token> tokens = tokenStream.getTokens();
        int[] lineNumber = new int[1];
        sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            final int tokenIndex = i;
            if (token.getType() == Token.EOF) {
                continue;
            }

            if (isMultiLineToken(token)) {
                styleMultiLineToken(token, lineNumber, sb, theme.gutter(), (fragment, builder) ->
                        applyStyles(fragment, builder, tokens, tokenIndex));
            } else {
                applyStyles(token, sb, tokens, tokenIndex);
            }
        }
    }

    void applyWithoutLines(StyleBuilder sb, BufferedTokenStream tokenStream) {
        List<Token> tokens = tokenStream.getTokens();
        for (int i = 0; i < tokens.size(); i++) {
            applyStyles(tokens.get(i), sb, tokens, i);
        }
    }

    void applyStyles(Token token, StyleBuilder sb, List<Token> tokens, int index) {
        if (token.getType() == Token.EOF) return;

        String text = token.getText();

        System.out.printf(
                "TEXT=[%s] TYPE=%d SYMBOLIC=%s%n",
                token.getText(),
                token.getType(),
                VOCABULARY.getSymbolicName(token.getType())
        );

        if (isComment(token)) {
            sb.appendAndReset(text, theme.comment());
        } else if (isKeyword(token)) {
            sb.appendAndReset(text, theme.keyword());
        } else if (isBraceString(token)) {
            appendBraceString(text, sb);
        } else if (isString(token)) {
            sb.appendAndReset(text, theme.stringLiteral());
        } else if (isNumber(token)) {
            sb.appendAndReset(text, theme.numberLiteral());
        } else if (isCiteKey(token, tokens, index)) {
            appendCiteKey(text, sb);
        } else if (isFieldName(token, tokens, index)) {
            sb.appendAndReset(text, theme.method());
        } else if (isName(token)) {
            sb.appendAndReset(text, theme.types());
        } else {
            sb.appendAndReset(text);
        }
    }

    void appendCiteKey(String text, StyleBuilder sb) {
        int colonIndex = text.indexOf(':');

        if (colonIndex < 0) {
            sb.appendAndReset(text, theme.constants());
            return;
        }

        sb.appendAndReset(
                text.substring(0, colonIndex + 1),
                theme.constants()
        );

        sb.appendAndReset(
                text.substring(colonIndex + 1),
                theme.method()
        );
    }

    boolean isKeyword(Token token) {
        int type = token.getType();
        return type == AT_STRING || type == AT_PREAMBLE || type == AT_COMMENT || type == AT_ENTRY;
    }

    boolean isString(Token token) {
        int type = token.getType();
        return type == DQUOTE_STRING;
    }

    boolean isBraceString(Token token) {
        return token.getType() == BRACE_STRING;
    }

    boolean isNumber(Token token) {
        return token.getType() == NUMBER;
    }

    boolean isComment(Token token) {
        return token.getType() == LINE_COMMENT;
    }

    boolean isName(Token token) {
        return token.getType() == NAME_TOKEN;
    }

    boolean isCiteKey(Token token, List<Token> tokens, int index) {
        if (!isName(token)) return false;

        Token prev = previousDefaultToken(tokens, index);
        return prev != null && (prev.getType() == LBRACE || prev.getType() == LPAREN);
    }

    boolean isFieldName(Token token, List<Token> tokens, int index) {
        if (!isName(token)) return false;
        if (isCiteKey(token, tokens, index)) return false;

        Token next = nextDefaultToken(tokens, index);
        return next != null && next.getType() == EQUALS;
    }

    boolean isMultiLineToken(Token token) {
        return token.getType() != Token.EOF && token.getText().contains(Constants.NEWLINE);
    }

    void appendBraceString(String text, StyleBuilder sb) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '{' || c == '}') {
                sb.appendAndReset(String.valueOf(c));
            } else {
                sb.appendAndReset(String.valueOf(c), theme.stringLiteral());
            }
        }
    }

    Token previousDefaultToken(List<Token> tokens, int index) {
        for (int i = index - 1; i >= 0; i--) {
            Token candidate = tokens.get(i);
            if (candidate.getType() == Token.EOF) continue;
            if (candidate.getChannel() == Token.DEFAULT_CHANNEL) return candidate;
        }
        return null;
    }

    Token nextDefaultToken(List<Token> tokens, int index) {
        for (int i = index + 1; i < tokens.size(); i++) {
            Token candidate = tokens.get(i);
            if (candidate.getType() == Token.EOF) continue;
            if (candidate.getChannel() == Token.DEFAULT_CHANNEL) return candidate;
        }
        return null;
    }
}