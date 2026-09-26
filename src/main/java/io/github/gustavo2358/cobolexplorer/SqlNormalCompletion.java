package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Closed SELECT INTO syntax. Authorizes successful return only; effects stay unknown. */
final class SqlNormalCompletion {
    static boolean proved(Ast.Statement statement) {
        return statement instanceof Ast.EmbeddedLanguageStatement s
            && s.language()==Ast.EmbeddedLanguage.SQL && selectInto(s.rawText());
    }
    static boolean selectInto(String raw) {
        try { return new Parser(raw).parse(); }
        catch (Unproved ignored) { return false; }
    }
    private static final class Unproved extends RuntimeException {
        Unproved(){super(null,null,false,false);}
    }
    private record Token(String text,boolean literal) { }
    private static final class Parser {
        private static final Set<String> RESERVED=Set.of("EXEC","SQL","SELECT","INTO","FROM","FETCH","FIRST","ROW","ROWS","ONLY","END-EXEC","WHERE","WHENEVER","AS","DISTINCT","ALL","NULL","CURRENT","DATE","TIME","TIMESTAMP","WITH","JOIN","ORDER","GROUP");
        final List<Token> tokens=new ArrayList<>();int index;
        Parser(String raw) {
            int i=0;while(i<raw.length()&&Character.isWhitespace(raw.charAt(i)))i++;
            if(raw.regionMatches(true,i,"*>EXECSQL",0,9))i+=9;
            while(i<raw.length()) {
                char c=raw.charAt(i);if(Character.isWhitespace(c)){i++;continue;}
                if(c=='\'') {
                    int start=i++;boolean ended=false;
                    while(i<raw.length())if(raw.charAt(i++)=='\'') {
                        if(i<raw.length()&&raw.charAt(i)=='\''){i++;continue;}
                        ended=true;break;
                    }
                    if(!ended)throw new Unproved();tokens.add(new Token(raw.substring(start,i),true));continue;
                }
                if(Character.isLetterOrDigit(c)||c=='_') {
                    int start=i++;
                    while(i<raw.length()&&(Character.isLetterOrDigit(raw.charAt(i))||raw.charAt(i)=='_'||raw.charAt(i)=='-'))i++;
                    tokens.add(new Token(raw.substring(start,i).toUpperCase(Locale.ROOT),false));continue;
                }
                if(".,:+-".indexOf(c)<0)throw new Unproved();
                tokens.add(new Token(String.valueOf(c),false));i++;
            }
        }
        boolean parse() {
            need("EXEC");need("SQL");need("SELECT");int selected=0;
            do {scalar();selected++;}while(take(","));
            need("INTO");int assigned=0;
            do {need(":");identifier(true);assigned++;}while(take(","));
            need("FROM");identifier(false);if(take("."))identifier(false);
            if(take("FETCH")) {
                need("FIRST");String count=next().text();
                if(!digits(count)||count.chars().allMatch(c->c=='0'))throw new Unproved();
                if(!take("ROW"))need("ROWS");need("ONLY");
            }
            need("END-EXEC");take(".");return index==tokens.size()&&selected==assigned;
        }
        void scalar() {
            if(index>=tokens.size())throw new Unproved();
            if(tokens.get(index).literal()){index++;return;}
            if(take("+")||take("-")){if(!digits(next().text()))throw new Unproved();return;}
            if(digits(tokens.get(index).text())){index++;return;}
            identifier(false);if(take("."))identifier(false);
        }
        void identifier(boolean host) {
            var t=next();String v=t.text();
            if(t.literal()||v.isEmpty()||RESERVED.contains(v)||!(v.charAt(0)>='A'&&v.charAt(0)<='Z'))throw new Unproved();
            for(int i=1;i<v.length();i++){char c=v.charAt(i);if(!(c>='A'&&c<='Z'||c>='0'&&c<='9'||c=='_'||host&&c=='-'))throw new Unproved();}
        }
        Token next(){if(index>=tokens.size())throw new Unproved();return tokens.get(index++);}
        boolean take(String text){if(index<tokens.size()&&!tokens.get(index).literal()&&tokens.get(index).text().equals(text)){index++;return true;}return false;}
        void need(String text){if(!take(text))throw new Unproved();}
        boolean digits(String s){return !s.isEmpty()&&s.chars().allMatch(c->c>='0'&&c<='9');}
    }
}
