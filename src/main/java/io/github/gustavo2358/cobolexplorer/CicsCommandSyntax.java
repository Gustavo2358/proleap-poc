package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Shared syntax only; each CICS domain owns option meaning. Linear, quote-aware scanner. */
public final class CicsCommandSyntax {
    private CicsCommandSyntax() { }
    public record Option(String name, Optional<String> operand, int start, int end) { }
    public record Command(String name,List<Option> options,List<String> gaps,boolean ended) {
        public Command {options=List.copyOf(options);gaps=List.copyOf(gaps);}
    }
    public static Optional<Command> parse(String raw) {
        var cursor=new Cursor(raw);cursor.space();
        if(cursor.at("*>EXECCICS")){cursor.position+=10;cursor.space();}
        if(!cursor.word().equalsIgnoreCase("EXEC")||!cursor.word().equalsIgnoreCase("CICS"))return Optional.empty();
        String name=cursor.word().toUpperCase(Locale.ROOT);if(name.isEmpty())return Optional.empty();
        var options=new ArrayList<Option>();var gaps=new LinkedHashSet<String>();boolean ended=false;
        while(cursor.position<raw.length()) {
            cursor.space();int start=cursor.position;String option=cursor.word().toUpperCase(Locale.ROOT);
            if(option.equals("END-EXEC")){ended=true;break;}
            if(option.isEmpty()){gaps.add("CICS_INVALID_OPTION_SYNTAX");break;}
            cursor.space();Optional<String> operand=Optional.empty();
            if(cursor.at("(")) {
                int begin=++cursor.position,depth=1;char quote=0;
                while(cursor.position<raw.length()&&depth>0) {
                    char c=raw.charAt(cursor.position++);
                    if(quote!=0) {if(c==quote){if(cursor.position<raw.length()&&raw.charAt(cursor.position)==quote)cursor.position++;else quote=0;}}
                    else if(c=='\''||c=='"')quote=c;
                    else if(c=='(')depth++;else if(c==')')depth--;
                }
                if(depth!=0||quote!=0){gaps.add("CICS_TRUNCATED_OPERAND");options.add(new Option(option,Optional.empty(),start,cursor.position));break;}
                operand=Optional.of(raw.substring(begin,cursor.position-1));
            }
            options.add(new Option(option,operand,start,cursor.position));
        }
        cursor.space();if(cursor.at(".")){cursor.position++;cursor.space();}
        if(!ended||cursor.position!=raw.length())gaps.add("CICS_INCOMPLETE_PAYLOAD");
        return Optional.of(new Command(name,options,List.copyOf(gaps),ended));
    }
    public static Optional<String> literal(String value) {
        if(value.isEmpty()||(value.charAt(0)!='\''&&value.charAt(0)!='"'))return Optional.empty();
        char quote=value.charAt(0);var decoded=new StringBuilder();
        for(int i=1;i<value.length();i++) {
            char c=value.charAt(i);if(c!=quote){decoded.append(c);continue;}
            if(i+1<value.length()&&value.charAt(i+1)==quote){decoded.append(c);i++;continue;}
            return i==value.length()-1?Optional.of(decoded.toString()):Optional.empty();
        }
        return Optional.empty();
    }
    private static final class Cursor {
        final String raw;int position;Cursor(String raw){this.raw=Objects.requireNonNull(raw);}
        void space(){while(position<raw.length()&&Character.isWhitespace(raw.charAt(position)))position++;}
        boolean at(String text){return raw.regionMatches(true,position,text,0,text.length());}
        String word(){space();int begin=position;while(position<raw.length()){char c=raw.charAt(position);if(!Character.isLetterOrDigit(c)&&c!='-'&&c!='_')break;position++;}return raw.substring(begin,position);}
    }
}
