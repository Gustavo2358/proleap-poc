package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

final class Bindings {
    private Bindings() {}

    static GrammarBinding cobol() { return new GrammarBinding() {
        public String name() { return "COBOL grammar"; }
        public Lexer preprocessorLexer(CharStream i) { return new CobolPreprocessorLexer(i); }
        public Parser preprocessorParser(TokenStream i) { return new CobolPreprocessorParser(i); }
        public ParseTree preprocessorStart(Parser p) { return ((CobolPreprocessorParser)p).startRule(); }
        public Lexer cobolLexer(CharStream i) { return new CobolLexer(i); }
        public Parser cobolParser(TokenStream i) { return new CobolParser(i); }
        public ParseTree cobolStart(Parser p) { return ((CobolParser)p).startRule(); }
    }; }
}
