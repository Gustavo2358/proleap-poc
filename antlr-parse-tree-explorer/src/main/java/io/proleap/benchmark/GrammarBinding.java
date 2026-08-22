package io.proleap.benchmark;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

interface GrammarBinding {
    String name();
    Lexer preprocessorLexer(CharStream input);
    Parser preprocessorParser(TokenStream input);
    ParseTree preprocessorStart(Parser parser);
    Lexer cobolLexer(CharStream input);
    Parser cobolParser(TokenStream input);
    ParseTree cobolStart(Parser parser);
}
