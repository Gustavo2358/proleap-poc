package io.github.gustavo2358.cobolexplorer;

import java.util.Locale;

/** N-LR SC27-8713-03, 28 April 2026, ASSIGN pp.142–143.
 * IBM assignment-name words and alphanumeric literals are admitted; dialect filenames are not.
 * The optional documentary label ends in a hyphen; the terminal name contains no hyphen.
 * This interpretation proves an external name, never an allocation mechanism.
 */
public final class FileDeclarationSemantics {
    private FileDeclarationSemantics() { }
    static Ast.FileAssignment assignmentName(String original) {
        boolean literal = original.length() >= 2 && (original.charAt(0) == '\'' || original.charAt(0) == '"')
                && original.charAt(original.length() - 1) == original.charAt(0);
        String value = literal ? original.substring(1, original.length() - 1) : original;
        String name = value.substring(value.lastIndexOf('-') + 1);
        if (value.indexOf('_') >= 0) return new Ast.FileAssignment(Ast.AssignmentForm.OUTSIDE_N_LR, original, null);
        if (name.isEmpty() || name.length() > 8 || !letter(name.charAt(0)))
            return new Ast.FileAssignment(Ast.AssignmentForm.OUTSIDE_N_LR, original, null);
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!letter(c) && !(c >= '0' && c <= '9') && !(literal && (c == '@' || c == '#' || c == '$')))
                return new Ast.FileAssignment(Ast.AssignmentForm.OUTSIDE_N_LR, original, null);
        }
        return new Ast.FileAssignment(Ast.AssignmentForm.IBM_NAME, original, name.toUpperCase(Locale.ROOT));
    }
    private static boolean letter(char c) { return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'; }
}
