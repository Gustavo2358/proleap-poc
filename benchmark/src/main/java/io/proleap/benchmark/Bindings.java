package io.proleap.benchmark;

import io.proleap.benchmark.antlr.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

final class Bindings {
    private Bindings() {}

    static GrammarBinding cobol85() { return new GrammarBinding() {
        public String name() { return "grammars-v4 Cobol85"; }
        public Lexer preprocessorLexer(CharStream i) { return new Cobol85PreprocessorLexer(i); }
        public Parser preprocessorParser(TokenStream i) { return new Cobol85PreprocessorParser(i); }
        public ParseTree preprocessorStart(Parser p) { return ((Cobol85PreprocessorParser)p).startRule(); }
        public Lexer cobolLexer(CharStream i) { return new Cobol85Lexer(i); }
        public Parser cobolParser(TokenStream i) { return new Cobol85Parser(i); }
        public ParseTree cobolStart(Parser p) { return ((Cobol85Parser)p).startRule(); }
    }; }

    static GrammarBinding proleap() { return new GrammarBinding() {
        public String name() { return "ProLeap Cobol"; }
        public Lexer preprocessorLexer(CharStream i) { return new CobolPreprocessorLexer(i); }
        public Parser preprocessorParser(TokenStream i) { return new CobolPreprocessorParser(i); }
        public ParseTree preprocessorStart(Parser p) { return ((CobolPreprocessorParser)p).startRule(); }
        public Lexer cobolLexer(CharStream i) { return new CobolLexer(i); }
        public Parser cobolParser(TokenStream i) { return new CobolParser(i); }
        public ParseTree cobolStart(Parser p) { return ((CobolParser)p).startRule(); }
    }; }
}
