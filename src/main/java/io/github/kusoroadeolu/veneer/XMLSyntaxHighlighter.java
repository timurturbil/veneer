package io.github.kusoroadeolu.veneer;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.style.StyleBuilder;
import io.github.kusoroadeolu.veneer.theme.SyntaxTheme;
import io.github.kusoroadeolu.veneer.XMLLexer;
import io.github.kusoroadeolu.veneer.XMLParser;
import io.github.kusoroadeolu.veneer.utils.Constants;
import io.github.kusoroadeolu.veneer.utils.FragmentToken;
import io.github.kusoroadeolu.veneer.utils.Utils;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XMLSyntaxHighlighter extends AbstractSyntaxHighlighter {

    public XMLSyntaxHighlighter(SyntaxTheme theme, boolean showLineNumbers) {
        super(theme, showLineNumbers);
    }

    public XMLSyntaxHighlighter() {
    }

    public XMLSyntaxHighlighter(boolean showLineNumbers) {
        super(showLineNumbers);
    }

    public XMLSyntaxHighlighter(SyntaxTheme theme) {
        super(theme);
    }

    @Override
    public String highlight(String s) {
        if (Utils.isNullOrBlank(s)) return "";
        StyleBuilder sb = Clique.styleBuilder();

        XMLLexer lexer = new XMLLexer(CharStreams.fromString(s));
        CommonTokenStream stream = Utils.toCommonTokenStream(lexer);


        XMLParser parser = new XMLParser(stream);
        parser.removeErrorListeners();

        XMLParser.DocumentContext tree = parser.document();

        Set<Integer> attrNameIndices = new HashSet<>();
        new DefaultXMLBaseVisitor(attrNameIndices).visit(tree);


        if (showLineNumbers) applyWithLines(sb, stream, attrNameIndices);
        else applyWithoutLines(sb, stream, attrNameIndices);

        return sb.toString();
    }


    void applyWithoutLines(StyleBuilder sb, BufferedTokenStream stream, Set<Integer> attrNameIndices) {
        var tokens = stream.getTokens();
        for (Token t : tokens) {
            applyStyles(t, sb, attrNameIndices);
        }
    }

    void applyWithLines(StyleBuilder sb, BufferedTokenStream stream, Set<Integer> attrNameIndices) {
        int[] lineNumber = new int[1];
        sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());

        for (Token token : stream.getTokens()) {
            if (isMultiLineToken(token.getType())) {
                styleMultiLineToken(token, lineNumber, sb, attrNameIndices);
            } else if (token.getType() == XMLLexer.NEWLINE) {
                applyStyles(token, sb, attrNameIndices);
                sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());
            } else applyStyles(token, sb, attrNameIndices);
        }
    }

    public void styleMultiLineToken(Token token, int[] lineNumber, StyleBuilder sb, Set<Integer> indices) {
        List<String> lines = token.getText().lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.appendAndReset(Constants.NEWLINE);
                sb.appendAndReset(Utils.formatNoTo3dp(++lineNumber[0]), theme.gutter());
            }

            applyStyles(new FragmentToken(token, lines.get(i)), sb, indices);
        }
    }


    void applyStyles(Token token, StyleBuilder sb, Set<Integer> attrNameIndices) {
        int type = token.getType();
        var text = token.getText();

        if (isString(type)) {
            sb.appendAndReset(text, theme.stringLiteral());
        } else if (isComment(type)) {
            sb.appendAndReset(text, theme.comment());
        } else if (isConstant(type)) {
            sb.appendAndReset(text, theme.constants());
        } else if (isKeyword(type)) {
            sb.appendAndReset(text, theme.keyword());
        } else if (isName(type)) {
            if (attrNameIndices.contains(token.getTokenIndex())) {
                sb.appendAndReset(text, theme.constants());
            } else {
                sb.appendAndReset(text, theme.types());
            }
        } else if (type != XMLLexer.EOF) {
            sb.appendAndReset(text);
        }
    }


    static class DefaultXMLBaseVisitor extends io.github.kusoroadeolu.veneer.XMLParserBaseVisitor<Void> {

        private final Set<Integer> attrNameIndices;

        public DefaultXMLBaseVisitor(Set<Integer> attrNameIndices) {
            this.attrNameIndices = attrNameIndices;
        }

        public Void visitAttribute(XMLParser.AttributeContext ctx) {
            attrNameIndices.add(ctx.Name().getSymbol().getTokenIndex());
            return visitChildren(ctx);
        }
    }


    boolean isComment(int type) {
        return type == XMLLexer.COMMENT || type == XMLLexer.PI;
    }

    boolean isString(int type) {
        return type == XMLLexer.STRING || type == XMLLexer.CDATA;
    }

    boolean isMultiLineToken(int type) {
        return type == XMLLexer.CDATA || type == XMLLexer.TEXT || isComment(type);
    }

    boolean isConstant(int type) {
        return type == XMLLexer.EntityRef || type == XMLLexer.CharRef;
    }

    boolean isKeyword(int type) {
        return type == XMLLexer.XMLDeclOpen;
    }

    boolean isName(int type) {
        return type == XMLLexer.Name;
    }
}
