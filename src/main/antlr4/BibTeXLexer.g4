/*
 * BibTeX Lexer Grammar for ANTLR 4
 *
 * References:
 *   - https://docs.jabref.org/advanced/strings
 *   - http://www.bibtex.org/Format/
 *   - http://maverick.inria.fr/~Xavier.Decoret/resources/xdkbibtex/bibtex_summary.html
 *   - BibLaTeX manual (extended field set)
 *
 * Supports:
 *   - Entry types: @article, @book, @inproceedings, etc.
 *   - Special entries: @string, @preamble, @comment
 *   - Field delimiters: both { ... } and " ... "
 *   - Concatenation operator: #
 *   - Nested braces inside brace-delimited values
 *   - String references (macros / abbreviations)
 *   - Numbers as bare field values
 *   - Month abbreviations and other predefined strings
 *   - Line comments (% prefix — not standard BibTeX but widely supported)
 *   - Whitespace and newlines -> channel(HIDDEN)
 */

// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar BibTeXLexer;

@header {
package io.github.kusoroadeolu.veneer;
}

// ─── Special entry keywords ──────────────────────────────────────────────────
// These must come BEFORE the generic AT_ENTRY rule so ANTLR gives them priority.

AT_STRING   : '@' [Ss][Tt][Rr][Ii][Nn][Gg]    -> pushMode(ENTRY_BODY);
AT_PREAMBLE : '@' [Pp][Rr][Ee][Aa][Mm][Bb][Ll][Ee] -> pushMode(ENTRY_BODY);
AT_COMMENT  : '@' [Cc][Oo][Mm][Mm][Ee][Nn][Tt] -> pushMode(COMMENT_BODY);

// ─── Regular entry (e.g. @article, @book, @inproceedings …) ──────────────────
AT_ENTRY    : '@' NAME -> pushMode(ENTRY_BODY);

// ─── Percent-sign comment (non-standard but widely used, e.g. JabRef exports) ─
LINE_COMMENT : '%' ~[\r\n]* -> channel(HIDDEN);

// ─── Whitespace outside entries ───────────────────────────────────────────────
WS : [ \t\r\n]+ -> channel(HIDDEN);

// ─────────────────────────────────────────────────────────────────────────────
// Mode: ENTRY_BODY
//   Entered when we see @TYPE.
//   Handles the opening delimiter ({ or (), the cite-key, field names,
//   field values, and the closing delimiter.
// ─────────────────────────────────────────────────────────────────────────────
mode ENTRY_BODY;

LINE_COMMENT_ENTRY : '%' ~[\r\n]* -> type(LINE_COMMENT), channel(HIDDEN);

// Opening delimiter: @article{ or @article(
LBRACE_ENTRY : '{' -> type(LBRACE), pushMode(FIELD_AREA);
LPAREN_ENTRY : '(' -> type(LPAREN), pushMode(FIELD_AREA);

// Whitespace inside the header (between @TYPE and the opening brace)
WS_ENTRY : [ \t\r\n]+ -> channel(HIDDEN);

// ─────────────────────────────────────────────────────────────────────────────
// Mode: COMMENT_BODY
//   Entered when we see @comment.
//   The comment body is ignored, but we still need to track nested braces or
//   parentheses so the matching closing delimiter is consumed correctly.
// ─────────────────────────────────────────────────────────────────────────────
mode COMMENT_BODY;

COMMENT_OPEN_BRACE  : '{' COMMENT_BRACE_CONTENT* '}' -> channel(HIDDEN), popMode;
COMMENT_OPEN_PAREN  : '(' COMMENT_PAREN_CONTENT* ')' -> channel(HIDDEN), popMode;
COMMENT_BODY_WS     : [ \t\r\n]+                     -> channel(HIDDEN);
COMMENT_BODY_ANY    : .                               -> channel(HIDDEN);

fragment COMMENT_BRACE_CONTENT
    : '{' COMMENT_BRACE_CONTENT* '}'
    | ~[{}]
    ;

fragment COMMENT_PAREN_CONTENT
    : '(' COMMENT_PAREN_CONTENT* ')'
    | ~[()]
    ;

// ─────────────────────────────────────────────────────────────────────────────
// Mode: FIELD_AREA
//   Handles: cite-key, field-name = field-value pairs, commas, closing } or )
// ─────────────────────────────────────────────────────────────────────────────
mode FIELD_AREA;

LINE_COMMENT_FIELD : '%' ~[\r\n]* -> type(LINE_COMMENT), channel(HIDDEN);

// Closing delimiters — pop back to the DEFAULT_MODE
RBRACE : '}' -> popMode, popMode;   // pop FIELD_AREA, then ENTRY_BODY
RPAREN : ')' -> popMode, popMode;

COMMA  : ',';
EQUALS : '=';

// Concatenation operator
CONCAT : '#';

// ─── Field value: double-quoted string  "..." ─────────────────────────────────
// Quotes may contain nested braces and escaped characters; they cannot contain
// unescaped double-quotes (those terminate the string).
DQUOTE_STRING : '"' DQUOTE_CHAR* '"';

// ─── Field value: brace-delimited string  {...} ───────────────────────────────
// Braces nest arbitrarily. We use a lexer action to track nesting depth.
// The outermost { } are the delimiters; inner ones are literal characters.
BRACE_STRING : '{' BRACE_CONTENT* '}';

// ─── Field value: bare number (e.g. year = 2024) ─────────────────────────────
NUMBER : [0-9]+;

// ─── Cite-keys and field names / string reference names ──────────────────────
// BibTeX allows letters, digits, and the characters listed below in keys/names.
// Practically: everything except whitespace, comma, =, #, {, }, (, ), "
NAME_TOKEN : NAME;

// Whitespace inside field area
WS_FIELD : [ \t\r\n]+ -> channel(HIDDEN);

// ─────────────────────────────────────────────────────────────────────────────
// Fragments
// ─────────────────────────────────────────────────────────────────────────────

// NAME: an identifier-like sequence used for entry types, cite-keys,
//       field names, and string macro references.
// BibTeX is lenient: almost any non-whitespace, non-delimiter character is valid.
fragment NAME : [a-zA-Z_\-:.+/]([a-zA-Z0-9_\-:.+/'])*;

// Characters inside a double-quoted string.
// An escaped quote \" is allowed; a bare { ... } block is allowed inside quotes.
fragment DQUOTE_CHAR
    : ~["\\{}]                // any regular character except ", \, {, and }
    | '\\' .                  // any escaped character
    | '{' BRACE_CONTENT* '}'  // brace-delimited block; BRACE_CONTENT permits double quotes inside
    ;

// The contents of a brace-delimited value, handling arbitrary nesting.
fragment BRACE_CONTENT
    : '{' BRACE_CONTENT* '}'   // nested brace pair
    | '\\' .                   // TeX escape sequence inside braces (\", \&, \%, …)
    | ~[{}\\]                  // any other character
    ;

// ─── Token-type aliases (so the parser sees consistent names) ─────────────────
// We reuse these via -> type(X) directives above.
LBRACE : '{';   // never matched directly; used as target of -> type()
LPAREN : '(';   // same

