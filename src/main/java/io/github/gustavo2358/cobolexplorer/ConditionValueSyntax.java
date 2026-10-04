package io.github.gustavo2358.cobolexplorer;

import java.util.Locale;
import static io.github.gustavo2358.cobolexplorer.Ast.ConditionValueKind.*;

/** Decodes one grammar-proven literal at the syntax boundary; never statement text. */
final class ConditionValueSyntax {
    static Ast.ConditionValue literal(String token) {
        var text=AstBuilder.basicLogicalTextToken(token);
        if(text.isPresent())return new Ast.ConditionValue(TEXT,text.get().value());
        String upper=token.toUpperCase(Locale.ROOT);
        var figurative=switch(upper) {
            case "SPACE","SPACES"->SPACES;
            case "LOW-VALUE","LOW-VALUES"->LOW_VALUES;
            case "HIGH-VALUE","HIGH-VALUES"->HIGH_VALUES;
            case "ZERO","ZEROS","ZEROES"->ZERO;
            case "QUOTE","QUOTES"->QUOTE;
            default->null;
        };
        if(figurative!=null)return new Ast.ConditionValue(figurative,"");
        if(upper.startsWith("X'")||upper.startsWith("X\"")) {
            String hex=upper.substring(2,upper.length()-1);
            if(hex.length()%2==0&&hex.chars().allMatch(c->Character.digit(c,16)>=0))return new Ast.ConditionValue(HEX,hex);
        }
        if(upper.startsWith("ALL")) {
            var repeated=AstBuilder.basicLogicalTextToken(token.substring(3).strip());
            if(repeated.isPresent())return new Ast.ConditionValue(ALL_TEXT,repeated.get().value());
        }
        try { return new Ast.ConditionValue(NUMBER,new java.math.BigDecimal(token).stripTrailingZeros().toPlainString()); }
        catch(NumberFormatException ignored){return new Ast.ConditionValue(UNAVAILABLE,token);}
    }
    private ConditionValueSyntax() { }
}
