package io.github.gustavo2358.cobolexplorer;

import io.github.gustavo2358.cobolexplorer.antlr.CobolParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.*;
import static io.github.gustavo2358.cobolexplorer.antlr.CobolParser.*;

/** Experimental recursive-descent DATA parser. It emits typed declaration drafts,
 * then immutable Ast nodes, never ANTLR contexts for the accepted DATA body.
 * The flat origin ledger preserves the existing presentation/provenance contract;
 * it is not used to discover semantic structure. Admission is transactional. */
public final class DirectDataParser {
    private DirectDataParser() { }

    public static final class Session {
        private final IdentityHashMap<ParseTree, Result> results = new IdentityHashMap<>();
        private final Map<String, Integer> reasons = new TreeMap<>();
        private int fallbacks;
        public void read(CobolParser parser, ParserRuleContext division) {
            TokenStream input = parser.getInputStream();
            int checkpoint = input.index();
            if (parser.getNumberOfSyntaxErrors() != 0) { fallback("prior syntax error"); return; }
            try {
                Result result = new Reader(input).read();
                if (!result.sections.isEmpty()) results.put(division, result);
            } catch (Unsupported inputOutsideSlice) {
                input.seek(checkpoint);
                fallback(inputOutsideSlice.getMessage());
            }
        }
        private void fallback(String reason) { fallbacks++; reasons.merge(reason, 1, Integer::sum); }
        Result result(ParseTree context) { return results.get(context); }
        public int acceptedDivisions() { return results.size(); }
        public int fallbackDivisions() { return fallbacks; }
        public int entries() { return results.values().stream().mapToInt(r -> r.entryCount).sum(); }
        public Map<String, Integer> fallbackReasons() { return Map.copyOf(reasons); }
    }

    /** One preorder syntax-origin row, with no children or ANTLR context. */
    static final class Event {
        final String rule;
        final Token start;
        final int parent, depth;
        Token stop;
        int children, size;
        Event(String rule, Token start, int parent, int depth) {
            this.rule = rule; this.start = start; this.parent = parent; this.depth = depth;
        }
        String rule() { return rule; }
        Token start() { return start; }
        Token stop() { return stop; }
        int parent() { return parent; }
        int depth() { return depth; }
        int children() { return children; }
    }
    record Coverage(Ast.Node node, String writtenText) { }
    record Built(List<Ast.Node> sections, int nextId, List<Coverage> coverage,
                 List<SemanticCoverage.Diagnostic> diagnostics) { }
    private record Section(int origin, Ast.DataSectionKind kind, String name, List<Entry> entries, List<FileDraft> files) { }
    private record Entry(int origin, String level, String name, boolean filler, List<Clause> clauses) { }
    private sealed interface Clause permits Picture, Usage, Value, Redefines, Preserved, Occurs { int origin(); }
    private record Picture(int origin, int pictureOrigin, String spelling) implements Clause { }
    private record Usage(int origin, boolean display) implements Clause { }
    private record Value(int origin, List<Integer> intervals, String basicToken, List<Ast.ConditionRange> ranges, Optional<Ast.ConditionValue> falseValue) implements Clause { }
    private record Redefines(int origin, int nameOrigin, String name) implements Clause { }
    private record Preserved(int origin, boolean external, boolean global) implements Clause { }
    private record Reference(int origin, String name, List<Qualifier> qualifiers) { }
    private record Qualifier(int origin, int nameOrigin, String name, boolean in) { }
    private record Occurs(int origin, int minimumInteger, Reference minimumReference,
                          Integer maximum, Reference depending, List<Reference> keys,
                          List<Integer> indexes) implements Clause { }
    private record FileDraft(int origin, String name, Ast.FileKind kind, List<FileClause> clauses,
                             List<Entry> entries) { }
    private sealed interface FileClause permits RecordSize, Recording, FileVisibility { int origin(); }
    private record RecordSize(int origin, Ast.FileRecordForm form, Integer minimum, Integer maximum,
                              Reference depending) implements FileClause { }
    private record Recording(int origin, int mode) implements FileClause { }
    private record FileVisibility(int origin, boolean external) implements FileClause { }


    static final class Result {
        final List<Event> events;
        final List<Section> sections;
        final int entryCount;
        Result(List<Event> events, List<Section> sections, int entryCount) {
            this.events = List.copyOf(events); this.sections = List.copyOf(sections); this.entryCount = entryCount;
        }
        List<Event> events() { return events; }
        int sectionCount() { return sections.size(); }
        Built build(int firstAstId, int firstOriginId, SourceMap sourceMap) {
            return new Materializer(this, firstAstId, firstOriginId, sourceMap).build();
        }
    }

    private static final class Unsupported extends RuntimeException {
        Unsupported(String reason) { super(reason, null, false, false); }
    }
    private static final class Reader {
        final TokenStream input;
        final List<Event> events = new ArrayList<>();
        final Deque<Integer> scopes = new ArrayDeque<>();
        int entries;
        Reader(TokenStream input) { this.input = input; }
        Result read() {
            var sections = new ArrayList<Section>();
            while (is(FILE, WORKING_STORAGE, LINKAGE, LOCAL_STORAGE)) sections.add(section());
            // All other section forms, nested constructs and uncertain boundaries
            // return to the unmodified grammar at the original token position.
            if (!is(PROCEDURE, END, IDENTIFICATION, ID, Token.EOF)) reject("unsupported DATA boundary");
            return new Result(events, sections, entries);
        }
        Section section() {
            int wrapper = enter("dataDivisionSection");
            int type = la();
            String rule = type == FILE ? "fileSection" : type == WORKING_STORAGE ? "workingStorageSection" : type == LINKAGE ? "linkageSection" : "localStorageSection";
            int origin = enter(rule);
            take(); expect(SECTION); expect(DOT_FS);
            var declarations = new ArrayList<Entry>();
            var files = new ArrayList<FileDraft>();
            if (type == FILE) { while (is(FD, SD)) files.add(file()); }
            else while (entryStart()) declarations.add(entry());
            close(origin); close(wrapper);
            return new Section(origin, type == FILE ? Ast.DataSectionKind.FILE : type == WORKING_STORAGE ? Ast.DataSectionKind.WORKING_STORAGE
                    : type == LINKAGE ? Ast.DataSectionKind.LINKAGE : Ast.DataSectionKind.LOCAL_STORAGE,
                    type == FILE ? "File Section" : type == WORKING_STORAGE ? "Working Storage Section" : type == LINKAGE ? "Linkage Section" : "Local Storage Section", List.copyOf(declarations), List.copyOf(files));
        }
        Entry entry() {
            int wrapper = enter("dataDescriptionEntry");
            if (la() == EXECSQLLINE) {
                int origin = enter("dataDescriptionEntryExecSql");
                do { take(); } while (la() == EXECSQLLINE);
                optional(DOT_FS); close(origin); close(wrapper); entries++;
                return new Entry(origin, "SQL", "FILLER", true, List.of());
            }
            boolean condition = la() == LEVEL_NUMBER_88;
            int origin = enter(condition ? "dataDescriptionEntryFormat3" : "dataDescriptionEntryFormat1");
            String level = take().getText();
            int numericLevel;
            try { numericLevel = Integer.parseInt(level); }
            catch (NumberFormatException outsideRange) { throw new Unsupported("level outside direct range"); }
            if (!level.chars().allMatch(c -> c >= '0' && c <= '9')
                    || !(numericLevel >= 1 && numericLevel <= 49 || level.equals("77") || level.equals("88")))
                reject("level outside direct range");
            boolean filler = la() == FILLER;
            String name;
            if (filler && !condition) name = take().getText();
            else if (wordToken(la())) name = name(condition ? "conditionName" : "dataName").getText();
            else { if (condition) reject("missing condition name"); name = "FILLER"; filler = true; }
            var clauses = new ArrayList<Clause>();
            if (condition) clauses.add(value());
            else while (la() != DOT_FS) {
                if (is(PIC, PICTURE)) clauses.add(picture());
                else if (is(USAGE) || usageToken(la())) clauses.add(usage());
                else if (is(VALUE, VALUES)) clauses.add(value());
                else if (is(REDEFINES)) clauses.add(redefines());
                else if (is(OCCURS)) clauses.add(occurs());
                else if (is(EXTERNAL, GLOBAL, IS, JUST, JUSTIFIED, SYNC, SYNCHRONIZED, BLANK)) clauses.add(preserved());
                else if (literalToken(la()) || wordToken(la())) clauses.add(value());
                else reject("unsupported data clause: " + VOCABULARY.getSymbolicName(la()));
            }
            expect(DOT_FS); close(origin); close(wrapper); entries++;
            return new Entry(origin, level, filler ? "FILLER" : name, filler, List.copyOf(clauses));
        }
        boolean entryStart() { return is(INTEGERLITERAL, LEVEL_NUMBER_77, LEVEL_NUMBER_88, EXECSQLLINE); }
        int integer() {
            if (!integerToken(la())) reject("expected integer literal");
            int origin = enter("integerLiteral"); take(); close(origin); return origin;
        }
        Reference reference() {
            int origin = enter("qualifiedDataName"), format = enter("qualifiedDataNameFormat1");
            String name = name("dataName").getText(); var qualifiers = new ArrayList<Qualifier>();
            while (is(IN, OF)) {
                int wrapper = enter("qualifiedInData"), q = enter("inData");
                boolean in = take().getType() == IN; int value = events.size(); String spelling = name("dataName").getText();
                close(q); close(wrapper); qualifiers.add(new Qualifier(q, value, spelling, in));
            }
            // Table calls, function calls and other identifier formats remain transactional fallbacks.
            if (la() == LPARENCHAR) reject("subscripted DATA reference");
            close(format); close(origin); return new Reference(origin, name, List.copyOf(qualifiers));
        }
        Occurs occurs() {
            int origin = enter("dataOccursClause"); take(); int min = -1; Reference minimum = null;
            if (integerToken(la())) min = integer();
            else { int id = enter("identifier"); minimum = reference(); close(id); }
            Integer max = null; Reference depending = null;
            if (la() == TO) { int to = enter("dataOccursTo"); take(); max = integer(); close(to); }
            optional(TIMES);
            if (la() == DEPENDING) { int d = enter("dataOccursDepending"); take(); optional(ON); depending = reference(); close(d); }
            var keys = new ArrayList<Reference>(); var indexes = new ArrayList<Integer>();
            while (is(ASCENDING, DESCENDING, INDEXED)) {
                if (la() == INDEXED) {
                    int indexed = enter("dataOccursIndexed"); take(); optional(BY); optional(LOCAL);
                    do { indexes.add(events.size()); name("indexName"); } while (wordToken(la()));
                    close(indexed);
                } else {
                    int sort = enter("dataOccursSort"); take(); optional(KEY); optional(IS);
                    do { keys.add(reference()); } while (wordToken(la()));
                    close(sort);
                }
            }
            close(origin); return new Occurs(origin, min, minimum, max, depending, List.copyOf(keys), List.copyOf(indexes));
        }
        boolean fileClauseStart(int t) { return t == RECORDING || t == RECORD || t == IS || t == GLOBAL || t == EXTERNAL; }
        FileDraft file() {
            int origin = enter("fileDescriptionEntry"); var kind = take().getType() == FD ? Ast.FileKind.FD : Ast.FileKind.SD;
            String name = name("fileName").getText(); var clauses = new ArrayList<FileClause>();
            while (fileClauseStart(la()) || la() == DOT_FS && fileClauseStart(input.LA(2))) {
                optional(DOT_FS); int wrapper = enter("fileDescriptionEntryClause");
                if (la() == RECORD) clauses.add(recordSize());
                else if (la() == RECORDING) {
                    int clause = enter("recordingModeClause"); take(); optional(MODE); optional(IS);
                    int mode = events.size(); name("modeStatement"); close(clause); clauses.add(new Recording(clause, mode));
                } else {
                    boolean external = la() == EXTERNAL || la() == IS && input.LA(2) == EXTERNAL;
                    int clause = enter(external ? "externalClause" : "globalClause"); optional(IS); expect(external ? EXTERNAL : GLOBAL);
                    close(clause); clauses.add(new FileVisibility(clause, external));
                }
                close(wrapper);
            }
            expect(DOT_FS); var declarations = new ArrayList<Entry>();
            while (entryStart()) declarations.add(entry());
            close(origin); return new FileDraft(origin, name, kind, List.copyOf(clauses), List.copyOf(declarations));
        }
        RecordSize recordSize() {
            int origin = enter("recordContainsClause"); take();
            Ast.FileRecordForm form; Integer min = null, max = null; Reference depending = null;
            if (is(IS, VARYING)) {
                form = Ast.FileRecordForm.VARYING; int variant = enter("recordContainsClauseFormat2");
                optional(IS); expect(VARYING); optional(IN); optional(SIZE);
                if (is(FROM) || integerToken(la())) {
                    optional(FROM); min = integer();
                    if (la() == TO) { int to = enter("recordContainsTo"); take(); max = integer(); close(to); }
                    optional(CHARACTERS);
                }
                if (la() == DEPENDING) { take(); optional(ON); depending = reference(); }
                close(variant);
            } else {
                int offset = la() == CONTAINS ? 2 : 1;
                boolean range = input.LA(offset + 1) == TO;
                form = range ? Ast.FileRecordForm.RANGE : Ast.FileRecordForm.FIXED;
                int variant = enter(range ? "recordContainsClauseFormat3" : "recordContainsClauseFormat1");
                optional(CONTAINS); min = integer();
                if (range) { int to = enter("recordContainsTo"); expect(TO); max = integer(); close(to); }
                else max = min;
                optional(CHARACTERS); close(variant);
            }
            close(origin); return new RecordSize(origin, form, min, max, depending);
        }
        Picture picture() {
            int origin = enter("dataPictureClause"); take(); optional(IS);
            int body = enter("pictureString"); var spelling = new StringBuilder(); int count = 0;
            while (pictureToken(la())) {
                int chars = enter("pictureChars");
                if (integerToken(la())) { int integer = enter("integerLiteral"); spelling.append(take().getText()); close(integer); }
                else spelling.append(take().getText());
                close(chars); count++;
            }
            if (count == 0) reject("empty PIC"); close(body); close(origin);
            return new Picture(origin, body, spelling.toString());
        }
        Usage usage() {
            int origin = enter("dataUsageClause");
            if (optional(USAGE)) optional(IS);
            int type = la(); if (!usageToken(type)) reject("unsupported USAGE"); take();
            if (type == BINARY && is(TRUNCATED, EXTENDED)) take();
            close(origin); return new Usage(origin, type == DISPLAY);
        }
        Value value() {
            int origin = enter("dataValueClause"); if (is(VALUE, VALUES)) { take(); if (is(IS, ARE)) take(); }
            var intervals = new ArrayList<Integer>(); var ranges=new ArrayList<Ast.ConditionRange>(); String single = null;
            do {
                if (!intervals.isEmpty()) optional(COMMACHAR);
                int interval = enter("dataValueInterval"); int from = enter("dataValueIntervalFrom");
                String basic = la() == NONNUMERICLITERAL ? input.LT(1).getText() : null;
                Ast.ConditionValue first=new Ast.ConditionValue(Ast.ConditionValueKind.UNAVAILABLE,"");
                if (literalToken(la())) first=literal("dataValueLiteral");
                else if (wordToken(la())) { int word = enter("cobolWord"); take(); close(word); }
                else reject("expected VALUE operand");
                close(from);
                Optional<Ast.ConditionValue> last=Optional.empty();
                if (is(THROUGH, THRU)) { int to = enter("dataValueIntervalTo"); take(); last=Optional.of(literal()); close(to); basic = null; }
                ranges.add(new Ast.ConditionRange(first,last));
                close(interval); intervals.add(interval); single = intervals.size() == 1 ? basic : null;
            } while (!is(WHEN,SET,TO,FALSE)&&(is(COMMACHAR) || literalToken(la()) || wordToken(la())));
            Optional<Ast.ConditionValue> falseValue=Optional.empty();
            if(is(WHEN,SET,TO,FALSE)){optional(WHEN);optional(SET);optional(TO);expect(FALSE);optional(IS);falseValue=Optional.of(literal());}
            close(origin); return new Value(origin, List.copyOf(intervals), single,List.copyOf(ranges),falseValue);
        }
        Ast.ConditionValue literal(){return literal("literal");}
        Ast.ConditionValue literal(String rule) {
            int start=input.index();
            int literal = enter(rule); int type = la();
            if (type == NONNUMERICLITERAL) take();
            else if (figurative(type) || type == ALL) { int f = enter("figurativeConstant"); take(); if (type == ALL) literal(); close(f); }
            else if (integerToken(type) || type == NUMERICLITERAL) {
                int n = enter("numericLiteral");
                if (integerToken(type)) { int i = enter("integerLiteral"); take(); close(i); } else take(); close(n);
            } else if (is(TRUE, FALSE)) { if(rule.equals("dataValueLiteral")){if(la()==FALSE)reject("FALSE starts a false clause");take();}else{int b = enter("booleanLiteral"); take(); close(b);} }
            else reject("unsupported VALUE operand");
            close(literal);
            StringBuilder spelling=new StringBuilder();
            for(int i=start;i<input.index();i++){var token=input.get(i);if(token.getChannel()==Token.DEFAULT_CHANNEL)spelling.append(token.getText());}
            return ConditionValueSyntax.literal(spelling.toString());
        }
        Redefines redefines() {
            int origin = enter("dataRedefinesClause"); take();
            if (!wordToken(la())) reject("missing REDEFINES name");
            int nameOrigin = events.size(); String spelling = name("dataName").getText(); close(origin);
            return new Redefines(origin, nameOrigin, spelling);
        }
        Preserved preserved() {
            int token = la(), next = input.LA(2);
            String rule = token == GLOBAL || token == IS && next == GLOBAL ? "dataGlobalClause"
                    : token == EXTERNAL || token == IS && next == EXTERNAL ? "dataExternalClause"
                    : is(JUST, JUSTIFIED) ? "dataJustifiedClause" : is(SYNC, SYNCHRONIZED) ? "dataSynchronizedClause"
                    : token == BLANK ? "dataBlankWhenZeroClause" : null;
            if (rule == null) reject("unsupported IS clause");
            int origin = enter(rule);
            switch (rule) {
                case "dataGlobalClause" -> { optional(IS); expect(GLOBAL); }
                case "dataExternalClause" -> { optional(IS); expect(EXTERNAL); if (la() == BY) reject("EXTERNAL BY"); }
                case "dataJustifiedClause" -> { take(); optional(RIGHT); }
                case "dataSynchronizedClause" -> { take(); if (is(LEFT, RIGHT)) take(); }
                case "dataBlankWhenZeroClause" -> { take(); optional(WHEN); if (!is(ZERO, ZEROS, ZEROES)) reject("BLANK requires ZERO"); take(); }
                default -> throw new IllegalStateException(rule);
            }
            close(origin); return new Preserved(origin,
                    token == EXTERNAL || token == IS && next == EXTERNAL,
                    token == GLOBAL || token == IS && next == GLOBAL);
        }
        Token name(String rule) { if (!wordToken(la())) reject("expected " + rule); int n = enter(rule), word = enter("cobolWord"); Token token = take(); close(word); close(n); return token; }
        int enter(String rule) {
            int id = events.size(), parent = scopes.isEmpty() ? -1 : scopes.peek();
            if (parent >= 0) events.get(parent).children++;
            events.add(new Event(rule, input.LT(1), parent, scopes.size())); scopes.push(id); return id;
        }
        void close(int id) {
            if (scopes.pop() != id) throw new IllegalStateException("unbalanced origin ledger");
            var event = events.get(id); event.stop = input.LT(-1); event.size = events.size() - id;
        }
        Token take() {
            if (la() == Token.EOF) reject("unexpected EOF"); Token token = input.LT(1);
            int parent = scopes.peek(); events.get(parent).children++;
            Event event = new Event(null, token, parent, scopes.size()); event.stop = token; event.size = 1; events.add(event);
            input.consume(); return token;
        }
        void expect(int type) { if (la() != type) reject("expected " + VOCABULARY.getSymbolicName(type)); take(); }
        boolean optional(int type) { if (la() != type) return false; take(); return true; }
        int la() { return input.LA(1); }
        boolean is(int... types) { for (int type : types) if (la() == type) return true; return false; }
        void reject(String reason) { throw new Unsupported(reason); }
    }
    /** Exact token alternatives of Cobol.g4 cobolWord; verified by a grammar parity test. */
    private static boolean wordToken(int token) {
        return switch (token) {
            case IDENTIFIER, ABORT, AS, ASCII, ASSOCIATED_DATA, ASSOCIATED_DATA_LENGTH, ATTRIBUTE, AUTO, AUTO_SKIP, BACKGROUND_COLOR, BACKGROUND_COLOUR, BEEP,
                    BELL, BINARY, BIT, BLINK, BLOB, BOUNDS, CODEPAGE, CAPABLE, CCSVERSION, CHANGED, CHANNEL, CLOB,
                    CLOSE_DISPOSITION, COBOL, COMMITMENT, CONTROL_POINT, CONVENTION, CRUNCH, CURSOR, DBCLOB, DEFAULT, DEFAULT_DISPLAY, DEFINITION, DFHRESP,
                    DFHVALUE, DISK, DONTCARE, DOUBLE, EBCDIC, EMPTY_CHECK, ENTER, ENTRY_PROCEDURE, EOL, EOS, ERASE, ESCAPE,
                    EVENT, EXCLUSIVE, EXPORT, EXTENDED, FOREGROUND_COLOR, FOREGROUND_COLOUR, FULL, FUNCTIONNAME, FUNCTION_POINTER, GRID, HIGHLIGHT, IMPLICIT,
                    IMPORT, INTEGER, KEPT, KEYBOARD, LANGUAGE, LB, LD, LEFTLINE, LENGTH_CHECK, LIBACCESS, LIBPARAMETER, LIBRARY,
                    LIST, LOCAL, LONG_DATE, LONG_TIME, LOWER, LOWLIGHT, MMDDYYYY, NAME, NAMED, NATIONAL, NATIONAL_EDITED, NETWORK,
                    NO_ECHO, NUMERIC_DATE, NUMERIC_TIME, ODT, ORDERLY, OVERLINE, OWN, PASSWORD, PORT, PRINTER, PRIVATE, PROCESS,
                    PROGRAM, PROMPT, READER, REAL, RECEIVED, RECURSIVE, REF, REMOTE, REMOVE, REQUIRED, REVERSE_VIDEO, SAVE,
                    SECURE, SHARED, SHAREDBYALL, SHAREDBYRUNUNIT, SHARING, SHORT_DATE, SQL, SYMBOL, TASK, THREAD, THREAD_LOCAL, TIMER,
                    TODAYS_DATE, TODAYS_NAME, TRUNCATED, TYPEDEF, UNDERLINE, VIRTUAL, WAIT, YEAR, YYYYMMDD, YYYYDDD, ZERO_FILL -> true;
            default -> false;
        };
    }
    private static boolean integerToken(int t) { return t == INTEGERLITERAL || t == LEVEL_NUMBER_66 || t == LEVEL_NUMBER_77 || t == LEVEL_NUMBER_88; }
    private static boolean pictureToken(int t) {
        return integerToken(t) || switch (t) {
            case DOLLARCHAR, IDENTIFIER, NUMERICLITERAL, SLASHCHAR, COMMACHAR, DOT, COLONCHAR, ASTERISKCHAR,
                    DOUBLEASTERISKCHAR, LPARENCHAR, RPARENCHAR, PLUSCHAR, MINUSCHAR, LESSTHANCHAR, MORETHANCHAR -> true;
            default -> false;
        };
    }
    private static boolean figurative(int t) {
        return switch (t) { case HIGH_VALUE, HIGH_VALUES, LOW_VALUE, LOW_VALUES, NULL, NULLS, QUOTE, QUOTES, SPACE, SPACES, ZERO, ZEROS, ZEROES -> true; default -> false; };
    }
    private static boolean literalToken(int t) { return t == ALL || integerToken(t) || figurative(t) || t == NONNUMERICLITERAL || t == NUMERICLITERAL || t == TRUE || t == FALSE; }
    private static boolean usageToken(int t) {
        return switch (t) {
            case BINARY, BIT, COMP, COMP_1, COMP_2, COMP_3, COMP_4, COMP_5, COMPUTATIONAL, COMPUTATIONAL_1,
                    COMPUTATIONAL_2, COMPUTATIONAL_3, COMPUTATIONAL_4, COMPUTATIONAL_5, CONTROL_POINT, DATE, DISPLAY,
                    DISPLAY_1, DOUBLE, EVENT, FUNCTION_POINTER, INDEX, KANJI, LOCK, NATIONAL, PACKED_DECIMAL,
                    POINTER, PROCEDURE_POINTER, REAL, SQL, TASK -> true;
            default -> false;
        };
    }

    private static final class Materializer {
        final Result result; final int originBase; final SourceMap sourceMap; final UnicodeText source;
        final List<Coverage> coverage = new ArrayList<>();
        final List<SemanticCoverage.Diagnostic> diagnostics = new ArrayList<>();
        int nextId;
        Materializer(Result result, int firstId, int originBase, SourceMap sourceMap) {
            this.result = result; nextId = firstId; this.originBase = originBase; this.sourceMap = sourceMap; source = new UnicodeText(sourceMap.text());
        }
        Built build() {
            var sections = new ArrayList<Ast.Node>();
            for (Section section : result.sections) {
                Ast.Meta meta = meta(section.origin()); var children = new ArrayList<Ast.Node>();
                for (FileDraft file : section.files()) children.add(file(file));
                children.addAll(hierarchy(section.entries()));
                sections.add(new Ast.Section(meta, section.name(), section.kind(), children));
            }
            return new Built(List.copyOf(sections), nextId, List.copyOf(coverage), List.copyOf(diagnostics));
        }
        List<Ast.DataEntry> hierarchy(List<Entry> entries) {
            var roots = new ArrayList<DataDraft>(); Deque<Map.Entry<Integer, DataDraft>> stack = new ArrayDeque<>(); DataDraft previous = null;
            for (Entry entry : entries) {
                Ast.DataEntry node = entry(entry); int level = entry.level().equals("SQL") ? -1 : Integer.parseInt(entry.level()); var draft = new DataDraft(node);
                if (level == 88 && previous != null) { previous.children.add(draft); continue; }
                while (!stack.isEmpty() && stack.peek().getKey() >= level) stack.pop();
                if (stack.isEmpty() || level == 77) roots.add(draft); else stack.peek().getValue().children.add(draft);
                if (level >= 1 && level <= 49) stack.push(Map.entry(level, draft)); previous = draft;
            }
            return roots.stream().map(this::freeze).toList();
        }
        Ast.FileDescription file(FileDraft file) {
            Ast.Meta meta = meta(file.origin()); var records = new ArrayList<Ast.FileRecordClause>();
            var auxiliary = new ArrayList<Ast.FileAuxiliary>(); boolean external = false, global = false;
            for (FileClause clause : file.clauses()) if (clause instanceof RecordSize r)
                records.add(new Ast.FileRecordClause(meta(r.origin()), r.form(), integerValue(r.minimum()), integerValue(r.maximum()), Optional.ofNullable(r.depending()).map(this::reference)));
            for (FileClause clause : file.clauses()) {
                if (clause instanceof FileVisibility v) { external |= v.external(); global |= !v.external(); continue; }
                Ast.Meta own = meta(clause.origin()); var params = new ArrayList<Ast.FileAuxParameter>(); var refs = new ArrayList<Ast.FileAuxData>(); Ast.FileAuxKind kind;
                if (clause instanceof Recording r) { kind = Ast.FileAuxKind.RECORDING_MODE; params.add(new Ast.FileAuxParameter("mode", text(r.mode()).strip())); }
                else if (clause instanceof RecordSize r) {
                    kind = Ast.FileAuxKind.RECORD; params.add(new Ast.FileAuxParameter("form", r.form().name()));
                    if (r.minimum() != null) params.add(new Ast.FileAuxParameter("minimum", text(r.minimum()).strip()));
                    if (r.maximum() != null && r.form() != Ast.FileRecordForm.FIXED) params.add(new Ast.FileAuxParameter("maximum", text(r.maximum()).strip()));
                    if (r.depending() != null) refs.add(new Ast.FileAuxData("depending", reference(r.depending())));
                } else throw new IllegalStateException("unknown file clause");
                auxiliary.add(new Ast.FileAuxiliary(own, kind, List.of(), refs, params, Optional.empty(), Ast.FileTrigger.NONE));
            }
            var declarations = hierarchy(file.entries());
            return new Ast.FileDescription(meta, file.name(), file.kind(), visibility(meta, external, global), declarations, records, auxiliary);
        }
        Optional<java.math.BigInteger> integerValue(Integer origin) { return origin == null ? Optional.empty() : Optional.of(new java.math.BigInteger(text(origin).strip())); }
        Ast.DataReference reference(Reference r) {
            Ast.Meta own = meta(r.origin()); var qualifiers = new ArrayList<Ast.DataQualifier>();
            for (int i = 0; i < r.qualifiers().size(); i++) {
                Qualifier q = r.qualifiers().get(i); Ast.Meta qm = meta(q.origin());
                var ref = new Ast.DataReference(meta(q.nameOrigin()), q.name(), text(q.nameOrigin()).strip(), List.of(), List.of(), null, Ast.ReferenceUnderstanding.STRUCTURED);
                qualifiers.add(new Ast.DataQualifier(qm, q.in() ? Ast.QualifierConnector.IN : Ast.QualifierConnector.OF,
                        i == r.qualifiers().size() - 1 ? Ast.QualifierTarget.DATA_OR_FILE : Ast.QualifierTarget.DATA, ref, text(q.origin()).strip()));
            }
            return new Ast.DataReference(own, r.name(), text(r.origin()).strip(), qualifiers, List.of(), null, Ast.ReferenceUnderstanding.STRUCTURED);
        }
        Ast.DeclarationVisibility visibility(Ast.Meta meta, boolean external, boolean global) {
            if (external && global) diagnostics.add(new SemanticCoverage.Diagnostic("CONFLICTING_DECLARATION_VISIBILITY", "Declaration contains both GLOBAL and EXTERNAL visibility", meta));
            return external && global ? Ast.DeclarationVisibility.CONFLICTING : external ? Ast.DeclarationVisibility.EXTERNAL : global ? Ast.DeclarationVisibility.GLOBAL : Ast.DeclarationVisibility.LOCAL;
        }
        private static final class DataDraft { final Ast.DataEntry entry; final List<DataDraft> children = new ArrayList<>(); DataDraft(Ast.DataEntry entry) { this.entry = entry; } }
        Ast.DataEntry freeze(DataDraft draft) {
            var e = draft.entry; return new Ast.DataEntry(e.meta(), e.level(), e.levelKind(), e.name(), e.filler(), e.visibility(), e.declaration(), e.clauses(), draft.children.stream().map(this::freeze).toList());
        }
        Ast.DataEntry entry(Entry entry) {
            Ast.Meta meta = meta(entry.origin()); var clauses = new ArrayList<Ast.DataClause>();
            boolean external = false, global = false;
            for (Clause draft : entry.clauses()) {
                clauses.add(clause(draft));
                if (draft instanceof Preserved preserved) {
                    external |= preserved.external(); global |= preserved.global();
                }
            }
            var visibility = visibility(meta, external, global);
            var kind = entry.level().equals("SQL") ? Ast.DataLevelKind.OPAQUE : entry.level().equals("88") ? Ast.DataLevelKind.CONDITION_88 : entry.level().equals("77") ? Ast.DataLevelKind.STANDALONE_77 : Ast.DataLevelKind.GROUP_OR_ELEMENTARY;
            var node = new Ast.DataEntry(meta, entry.level(), kind, entry.name(), entry.filler(), visibility, compact(text(entry.origin())), clauses, List.of());
            coverage.add(new Coverage(node, text(entry.origin()))); return node;
        }
        Ast.DataClause clause(Clause draft) {
            Ast.Meta meta = meta(draft.origin()); String text = text(draft.origin()).strip(); Ast.DataClause node;
            if (draft instanceof Picture p) node = new Ast.PictureClause(meta, text(p.pictureOrigin()).strip(), text,
                    AstBuilder.elementaryTextExtent(p.spelling()), AstBuilder.elementaryIntegerDigits(p.spelling()));
            else if (draft instanceof Usage u) node = new Ast.UsageClause(meta, text.replaceFirst("(?i)^USAGE\\s+(IS\\s+)?", ""), text, u.display());
            else if (draft instanceof Value v) node = new Ast.ValueClause(meta, v.intervals().stream().map(this::text).map(String::strip).toList(), text,
                    v.basicToken() == null ? Optional.empty() : AstBuilder.basicLogicalTextToken(v.basicToken()),v.ranges(),v.falseValue());
            else if (draft instanceof Redefines r) node = new Ast.RedefinesClause(meta, new Ast.DataReference(meta(r.nameOrigin()), r.name(), text(r.nameOrigin()).strip(), List.of(), List.of(), null, Ast.ReferenceUnderstanding.STRUCTURED), text);
            else if (draft instanceof Occurs o) {
                Ast.Expression minimum = o.minimumReference() == null ? integerExpression(o.minimumInteger()) : reference(o.minimumReference());
                Ast.Expression maximum = o.maximum() == null ? null : integerExpression(o.maximum());
                Ast.DataReference depending = o.depending() == null ? null : reference(o.depending());
                var keys = o.keys().stream().map(this::reference).toList();
                var indexes = o.indexes().stream().map(i -> new Ast.IndexReference(meta(i), text(i).strip(), text(i).strip())).toList();
                node = new Ast.OccursClause(meta, minimum, maximum, depending, keys, indexes, text);
            }
            else node = new Ast.PreservedDataClause(meta, result.events.get(draft.origin()).rule, text, List.of());
            coverage.add(new Coverage(node, text(draft.origin()))); return node;
        }
        Ast.LiteralExpression integerExpression(int origin) {
            String text = text(origin).strip();
            return new Ast.LiteralExpression(meta(origin), text, text, Optional.empty(), integerValue(origin));
        }
        Ast.Meta meta(int origin) {
            Event event = result.events.get(origin); Token start = event.start, stop = event.stop;
            var span = new Ast.SourceSpan(start.getLine(), start.getCharPositionInLine(), stop.getLine(), stop.getCharPositionInLine() + Math.max(0, stop.getText().codePointCount(0, stop.getText().length()) - 1), start.getTokenIndex(), stop.getTokenIndex());
            int begin = Math.max(0, start.getStartIndex()), end = Math.min(source.length(), stop.getStopIndex() + 1);
            return new Ast.Meta(nextId++, span, new Ast.ParseTreeOrigin(originBase + origin, event.rule, event.size), sourceMap.provenance(begin, end), sourceMap.syntheticModel(begin, end));
        }
        String text(int origin) { Event e = result.events.get(origin); return source.substring(Math.max(0, e.start.getStartIndex()), Math.min(source.length(), e.stop.getStopIndex() + 1)); }
        String compact(String text) { return text.replaceAll("\\s+", " ").trim(); }
    }
}
