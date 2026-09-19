package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Conservative table-position extractor; no SQL AST, expression semantics or catalog resolution. */
final class Db2SourceExtractor {
    record Table(String schema,String name,String operation,String access) {}
    record Result(List<Table> tables,List<String> gaps) {}
    private record Token(String text,boolean word) {}
    private static final class Unproved extends RuntimeException {
        final String gap;
        Unproved(String gap){super(null,null,false,false);this.gap=gap;}
    }
    private static final Set<String> RESERVED=Set.of("SELECT","FROM","JOIN","LEFT","RIGHT","FULL","INNER","OUTER","CROSS","ON","WHERE","GROUP","ORDER","HAVING","FETCH","OFFSET","UNION","EXCEPT","INTERSECT","WITH","AS","INSERT","INTO","VALUES","UPDATE","SET","DELETE","MERGE","USING","WHEN","THEN","MATCHED","NOT","FOR","TABLE","LATERAL","FINAL","OLD","NEW","ONLY","RECURSIVE","END","AND","OR");
    private final List<Token> tokens;
    private final int[] close;
    private final List<Table> tables=new ArrayList<>();
    private Db2SourceExtractor(List<Token> tokens) {
        this.tokens=tokens;close=new int[tokens.size()];Arrays.fill(close,-1);var stack=new ArrayDeque<Integer>();
        for(int i=0;i<tokens.size();i++)if(at(i,"("))stack.push(i);else if(at(i,")")){if(stack.isEmpty())fail();close[stack.pop()]=i;}
        if(!stack.isEmpty())fail();
    }
    static Result extract(String sql) {
        try {
            var x=new Db2SourceExtractor(tokenize(sql));
            if(x.tokens.isEmpty()||x.tokens.stream().anyMatch(t->t.text().equals(";")))fail();
            if(x.at(0,"PREPARE")||x.at(0,"EXECUTE"))throw new Unproved("DYNAMIC_SQL_NOT_ANALYZED");
            // Existing INCLUDE classification remains the sole authority for these facts.
            if((x.at(0,"BEGIN")||x.at(0,"END"))&&x.tokens.size()==3&&x.at(1,"DECLARE")&&x.at(2,"SECTION"))return new Result(List.of(),List.of());
            if(x.at(0,"INCLUDE")||x.at(0,"WHENEVER")||x.at(0,"COMMIT")||x.at(0,"ROLLBACK")||x.at(0,"OPEN")||x.at(0,"CLOSE")||x.at(0,"FETCH"))return new Result(List.of(),List.of());
            x.statement(0,x.tokens.size(),Set.of(),0);
            return new Result(List.copyOf(x.tables),List.of());
        } catch(Unproved e){return new Result(List.of(),List.of(e.gap));}
    }
    private static void fail(){throw new Unproved("DB2_STATIC_SQL_UNPROVED");}
    private boolean at(int i,String s){return i<tokens.size()&&tokens.get(i).text().equals(s);}
    private int expect(int i,int end,String word){if(i>=end||!at(i,word))fail();return i+1;}
    private boolean identifier(int i,int end){return i<end&&tokens.get(i).word()&&!RESERVED.contains(tokens.get(i).text());}
    private int identifierEnd(int i,int end){if(!identifier(i,end))fail();int next=i+1;if(next<end&&at(next,".")){if(!identifier(next+1,end))fail();next+=2;}if(next<end&&at(next,"."))fail();return next;}
    private int groupEnd(int i,int end){if(i>=end||!at(i,"(")||close[i]<0||close[i]>=end)fail();return close[i];}
    private int alias(int i,int end){if(i<end&&at(i,"AS")){i++;if(!identifier(i,end))fail();return i+1;}return identifier(i,end)?i+1:i;}
    private int relation(int i,int end,Set<String> ctes,String operation,String access,int depth,boolean target) {
        if(i<end&&at(i,"(")) {
            if(target)fail();int stop=groupEnd(i,end);statement(i+1,stop,ctes,depth+1);return alias(stop+1,end);
        }
        int next=identifierEnd(i,end);String schema=next-i==3?tokens.get(i).text():"";String name=tokens.get(next-1).text();
        if(next<end&&at(next,"(")&&!target)fail(); // Table functions are not base relation names.
        if(schema.isEmpty()&&ctes.contains(name)) {if(target)fail();}
        else tables.add(new Table(schema,name,operation,access));
        return target?next:alias(next,end);
    }
    private void statement(int start,int end,Set<String> outer,int depth) {
        if(depth>128||start>=end)fail();int i=start;Set<String> ctes=outer;
        if(at(i,"WITH")) {
            i++;if(at(i,"RECURSIVE"))throw new Unproved("DB2_RECURSIVE_CTE_UNSUPPORTED");
            var local=new HashSet<>(outer);var bodies=new ArrayList<int[]>();var names=new HashMap<String,Integer>();
            while(true) {
                if(!identifier(i,end))fail();String name=tokens.get(i++).text();if(!local.add(name))fail();names.put(name,bodies.size());
                if(i<end&&at(i,"("))i=groupEnd(i,end)+1;
                i=expect(i,end,"AS");int stop=groupEnd(i,end);bodies.add(new int[]{i+1,stop});i=stop+1;
                if(i>=end||!at(i,","))break;i++;
            }
            // All local CTE names are known before traversing definitions, preventing fabricated external names.
            for(int b=0;b<bodies.size();b++) {
                int[] bounds=bodies.get(b);
                for(int k=bounds[0];k<bounds[1];k++)if((at(k,"FROM")||at(k,"JOIN"))&&k+1<bounds[1]&&names.getOrDefault(tokens.get(k+1).text(),-1)>=b)
                    throw new Unproved("DB2_RECURSIVE_CTE_UNSUPPORTED");
                statement(bounds[0],bounds[1],local,depth+1);
            }
            ctes=local;
        }
        if(i>=end)fail();
        if(at(i,"DECLARE")) {
            i++;if(!identifier(i,end))fail();i=expect(i+1,end,"CURSOR");i=expect(i,end,"FOR");statement(i,end,ctes,depth+1);return;
        }
        if(at(i,"SELECT")){select(i,end,ctes,depth);return;}
        if(at(i,"INSERT")) {
            i=expect(i+1,end,"INTO");i=relation(i,end,ctes,"INSERT","WRITE",depth,true);
            if(i<end&&at(i,"("))i=groupEnd(i,end)+1;
            if(i<end&&(at(i,"SELECT")||at(i,"WITH"))){statement(i,end,ctes,depth+1);return;}
            i=expect(i,end,"VALUES");if(i>=end)fail();expressions(i,end,ctes,depth);return;
        }
        if(at(i,"UPDATE")) {i=relation(i+1,end,ctes,"UPDATE","WRITE",depth,true);i=alias(i,end);i=expect(i,end,"SET");if(i>=end)fail();expressions(i,end,ctes,depth);return;}
        if(at(i,"DELETE")) {i=expect(i+1,end,"FROM");i=relation(i,end,ctes,"DELETE","WRITE",depth,true);i=alias(i,end);if(i<end&&!at(i,"WHERE"))fail();expressions(i,end,ctes,depth);return;}
        if(at(i,"MERGE")) {
            i=expect(i+1,end,"INTO");i=relation(i,end,ctes,"MERGE","READ_WRITE",depth,true);i=alias(i,end);i=expect(i,end,"USING");i=relation(i,end,ctes,"MERGE","READ",depth,false);i=expect(i,end,"ON");int when=findTop(i,end,Set.of("WHEN"));if(when<=i||when==end)fail();expressions(i,end,ctes,depth);return;
        }
        throw new Unproved("DB2_SQL_SHAPE_UNSUPPORTED");
    }
    private int findTop(int start,int end,Set<String> words) {
        for(int i=start;i<end;i++){if(words.contains(tokens.get(i).text()))return i;if(at(i,"("))i=groupEnd(i,end);}return end;
    }
    private void select(int start,int end,Set<String> ctes,int depth) {
        if(depth>128)fail();
        int union=findTop(start+1,end,Set.of("UNION","EXCEPT","INTERSECT"));
        if(union<end){select(start,union,ctes,depth+1);int n=union+1;if(at(n,"ALL")||at(n,"DISTINCT"))n++;statement(n,end,ctes,depth+1);return;}
        int from=findTop(start+1,end,Set.of("FROM"));if(from<=start+1||from==end)fail();expressions(start+1,from,ctes,depth);
        int i=relation(from+1,end,ctes,"SELECT","READ",depth,false);
        while(i<end) {
            if(at(i,",")){i=relation(i+1,end,ctes,"SELECT","READ",depth,false);continue;}
            if(Set.of("LEFT","RIGHT","FULL","INNER","CROSS").contains(tokens.get(i).text())) {i++;if(at(i,"OUTER"))i++;i=expect(i,end,"JOIN");i=relation(i,end,ctes,"SELECT","READ",depth,false);continue;}
            if(at(i,"JOIN")){i=relation(i+1,end,ctes,"SELECT","READ",depth,false);continue;}
            if(at(i,"ON")) {
                int next=findTop(i+1,end,Set.of("JOIN","LEFT","RIGHT","FULL","INNER","CROSS","WHERE","GROUP","ORDER","HAVING","FETCH","OFFSET"));if(next==i+1)fail();expressions(i+1,next,ctes,depth);i=next;continue;
            }
            if(Set.of("WHERE","GROUP","ORDER","HAVING","FETCH","OFFSET","FOR","WITH").contains(tokens.get(i).text())) {if(i+1>=end)fail();expressions(i+1,end,ctes,depth);return;}
            fail();
        }
    }
    private void expressions(int start,int end,Set<String> ctes,int depth) {
        if(depth>128)fail();
        for(int i=start;i<end;i++) {
            if(at(i,";"))fail();
            if(at(i,"WHERE")&&i+1==end)fail();
            if(at(i,"(")){int stop=groupEnd(i,end);if(at(i+1,"SELECT")||at(i+1,"WITH"))statement(i+1,stop,ctes,depth+1);else expressions(i+1,stop,ctes,depth+1);i=stop;}
            else if(at(i,"SELECT")||at(i,"FROM")||at(i,"JOIN"))fail();
        }
    }
    private static List<Token> tokenize(String sql) {
        var result=new ArrayList<Token>();int i=0;
        while(i<sql.length()) {
            char c=sql.charAt(i);if(Character.isWhitespace(c)){i++;continue;}
            if(c=='-'&&i+1<sql.length()&&sql.charAt(i+1)=='-'){while(i<sql.length()&&sql.charAt(i)!='\n')i++;continue;}
            if(c=='/'&&i+1<sql.length()&&sql.charAt(i+1)=='*'){int stop=sql.indexOf("*/",i+2);if(stop<0)fail();int nested=sql.indexOf("/*",i+2);if(nested>=0&&nested<stop)throw new Unproved("DB2_NESTED_COMMENT_UNSUPPORTED");i=stop+2;continue;}
            if(c=='\''||c=='"') {
                char quote=c;i++;boolean closed=false;while(i<sql.length()){if(sql.charAt(i++)==quote){if(i<sql.length()&&sql.charAt(i)==quote){i++;continue;}closed=true;break;}}
                if(!closed)fail();if(quote=='"')throw new Unproved("DB2_DELIMITED_IDENTIFIER_UNSUPPORTED");result.add(new Token("<literal>",false));continue;
            }
            if(c==':'){i++;int start=i;while(i<sql.length()&&(Character.isLetterOrDigit(sql.charAt(i))||"_-".indexOf(sql.charAt(i))>=0))i++;if(start==i)fail();result.add(new Token("<host>",false));continue;}
            if(Character.isLetter(c)||c=='_'||c=='$'||c=='#'||c=='@'){int start=i++;while(i<sql.length()&&(Character.isLetterOrDigit(sql.charAt(i))||"_$#@".indexOf(sql.charAt(i))>=0))i++;result.add(new Token(sql.substring(start,i).toUpperCase(Locale.ROOT),true));continue;}
            result.add(new Token(String.valueOf(c),false));i++;
        }
        return List.copyOf(result);
    }
}
