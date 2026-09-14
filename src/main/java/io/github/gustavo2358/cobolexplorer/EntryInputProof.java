package io.github.gustavo2358.cobolexplorer;

import java.util.HashMap;
import java.util.List;

/** Input occurrences proven to belong only to this entry's DATA region.
 * This proves neither declaration completeness nor storage independence. */
public record EntryInputProof(List<Diagnostic> dataCopies,List<Diagnostic> separateUnitCopies) {
    public EntryInputProof { dataCopies = List.copyOf(dataCopies);separateUnitCopies=List.copyOf(separateUnitCopies); }
    public EntryInputProof(List<Diagnostic> dataCopies) { this(dataCopies,List.of()); }

    public boolean unaffectedBy(ResolutionAnalysisReport.FrontendState input) {
        if (input.preprocessorErrors() != 0 || input.lexerErrors() != 0 || input.parserErrors() != 0)
            return false;
        var remaining = new HashMap<Diagnostic, Integer>();
        for (var d : dataCopies) remaining.merge(d, 1, Integer::sum);
        for (var d : separateUnitCopies) remaining.merge(d, 1, Integer::sum);
        for (var d : input.diagnostics()) {
            switch (d.phase()) {
                case PREPROCESSOR -> {
                    if (d.code() != Diagnostic.Code.UNRESOLVED_COPY || !remaining.containsKey(d)) return false;
                    if (remaining.get(d) == 1) remaining.remove(d);
                    else remaining.put(d, remaining.get(d) - 1);
                }
                case LEXER, PARSER, IO -> { return false; }
                case OTHER -> { }
            }
        }
        return remaining.isEmpty();
    }
}
