package io.github.kusoroadeolu.veneer;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.style.StyleBuilder;
import io.github.kusoroadeolu.veneer.theme.SyntaxTheme;
import io.github.kusoroadeolu.veneer.utils.Utils;
import org.antlr.v4.runtime.*;
import io.github.kusoroadeolu.veneer.JSONLexer;
import io.github.kusoroadeolu.veneer.JSONParser;


import java.util.HashSet;
import java.util.Set;

import static io.github.kusoroadeolu.veneer.utils.Utils.isNullOrBlank;


public class JSONSyntaxHighlighter extends AbstractSyntaxHighlighter implements SyntaxHighlighter{

    public JSONSyntaxHighlighter(SyntaxTheme theme, boolean showLineNumbers) {
        super(theme, showLineNumbers);
    }

    public JSONSyntaxHighlighter() {
    }

    public JSONSyntaxHighlighter(boolean showLineNumbers) {
        super(showLineNumbers);
    }

    public JSONSyntaxHighlighter(SyntaxTheme theme) {
        super(theme);
    }

    @Override
    public String highlight(String s) {
        if (isNullOrBlank(s)) return "";

        StyleBuilder sb = Clique.styleBuilder();

        JSONLexer lexer = new JSONLexer(CharStreams.fromString(s));

        CommonTokenStream tokens = Utils.toCommonTokenStream(lexer);

        JSONParser parser = new JSONParser(tokens);
        parser.removeErrorListeners();

        JSONParser.JsonContext tree = parser.json();

        Set<Integer> keyIndices = new HashSet<>();
        var visitor = new DefaultJSONBaseVisitor(keyIndices);
        visitor.visit(tree); //So we know the index of keys (styled as constants)

        if (showLineNumbers) applyWithLines(sb, tokens, keyIndices);
        else applyWithoutLines(sb, tokens, keyIndices);

        return sb.toString();
    }

    void applyWithoutLines(StyleBuilder sb, CommonTokenStream tokens, Set<Integer> keyIndices) {
        for (Token token : tokens.getTokens()) {
            applyStyles(token, sb, keyIndices);
        }
    }

    void applyWithLines(StyleBuilder sb, CommonTokenStream tokens, Set<Integer> keyIndices) {
        int[] lineNumber = new int[1];
        sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());
        var tokenList = tokens.getTokens();
        for (int index = 0, prev = -1; index < tokenList.size(); ++index, ++prev) {
            var token = tokenList.get(index);
            if (prev != -1) {
                var prevToken = tokenList.get(prev);
                if (prevToken.getType() == JSONLexer.NEWLINE) {
                    sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());
                }
            }
            applyStyles(token, sb, keyIndices);

        }
    }

//    void applyWithLines(StyleBuilder sb, CommonTokenStream tokens, Set<Integer> keyIndices) {
//        int[] lineNumber = new int[1];
//        int index = 0;
//        sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());
//        for (Token token : tokens.getTokens()) {
//            if (token.getType() == JSONLexer.WS) {
//                sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());
//            }
//            applyStyles(token, sb, keyIndices);
//
//        }
//    }

    void applyStyles(Token token, StyleBuilder sb, Set<Integer> keyIndices) {
        int type = token.getType();

        if (type == JSONLexer.NUMBER) {
            sb.appendAndReset(token.getText(), theme.numberLiteral());
        } else if (isKeyword(type)) {
            sb.appendAndReset(token.getText(), theme.keyword());
        } else if (type == JSONLexer.STRING) {
            if (keyIndices.contains(token.getTokenIndex())) sb.appendAndReset(token.getText(), theme.constants());
            else sb.appendAndReset(token.getText(), theme.stringLiteral());
        } else if (type != JSONLexer.EOF) {
            sb.append(token.getText());
        }
    }

    boolean isKeyword(int type) {
        return type == JSONLexer.T__6 || type == JSONLexer.T__7 || type == JSONLexer.T__8;
    }


    static class DefaultJSONBaseVisitor extends io.github.kusoroadeolu.veneer.JSONBaseVisitor<Void> {

        private final Set<Integer> keyIndices;

        public DefaultJSONBaseVisitor(Set<Integer> keyIndices) {
            this.keyIndices = keyIndices;
        }

        public Void visitPair(JSONParser.PairContext ctx) {
            keyIndices.add(ctx.STRING().getSymbol().getTokenIndex());
            return visitChildren(ctx);
        }
    }

}
