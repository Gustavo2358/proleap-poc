package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Independent CICS TS 5.6 API/SPI oracle; no SELECT or FD premise. */
class CicsFileControlTest {
    private static final Map<String,String> REQUIRED = Map.ofEntries(
        Map.entry("READ","INTO(BUF) RIDFLD(KEY)"), Map.entry("WRITE","FROM(BUF) RIDFLD(KEY)"),
        Map.entry("REWRITE","FROM(BUF)"), Map.entry("DELETE","RIDFLD(KEY)"),
        Map.entry("STARTBR","RIDFLD(KEY)"), Map.entry("READNEXT","INTO(BUF) RIDFLD(KEY)"),
        Map.entry("READPREV","INTO(BUF) RIDFLD(KEY)"), Map.entry("RESETBR","RIDFLD(KEY)"),
        Map.entry("ENDBR","REQID(1)"), Map.entry("UNLOCK","TOKEN(TOK)"),
        Map.entry("INQUIRE","OPENSTATUS(ST)"), Map.entry("SET","OPEN"));
    private CicsFileControlAnalyzer.Fact parse(String command) {
        return new CicsFileControlAnalyzer().parse("EXEC CICS "+command+" END-EXEC").orElseThrow();
    }
    @Test void twelveCommandsHaveExplicitTargetsAndRejectMissingOrDuplicatedIdentity() {
        for(var entry:REQUIRED.entrySet()) {
            var command=entry.getKey();var tail=entry.getValue();
            var good=parse(command+" FILE('ACCOUNTS') "+tail+" NOHANDLE");
            assertEquals(command,good.command().name());assertEquals("INPUT",good.targetMode().name());
            assertEquals("ACCOUNTS",good.literal().orElseThrow());assertTrue(good.gaps().isEmpty(),good.toString());
            assertFalse(parse(command+" FILE('A') FILE('B') "+tail).gaps().isEmpty(),command);
            // FILE keyword with missing operand is still an observed FILE fact, never silent omission.
            assertFalse(parse(command+" FILE "+tail).gaps().isEmpty(),command);
        }
    }
    @Test void optionDirectionsComeFromEachCommandAndNeverFromDataAreaSpelling() {
        assertRole("READ FILE('F') INTO(B) RIDFLD(K) LENGTH(L) TOKEN(T)","LENGTH","READ_WRITE");
        assertRole("READ FILE('F') INTO(B) RIDFLD(K) TOKEN(T)","TOKEN","WRITE");
        assertRole("READ FILE('F') INTO(B) RIDFLD(K)","RIDFLD","READ");
        assertRole("READ FILE('F') INTO(B) RIDFLD(K) GENERIC KEYLENGTH(4)","RIDFLD","READ_WRITE");
        for(String command:List.of("READNEXT","READPREV")) {
            assertRole(command+" FILE('F') SET(P) RIDFLD(K) TOKEN(T)","RIDFLD","READ_WRITE");
            assertRole(command+" FILE('F') SET(P) RIDFLD(K) TOKEN(T)","SET","WRITE");
        }
        for(String command:List.of("REWRITE","DELETE","UNLOCK"))assertRole(command+" FILE('F') TOKEN(T)","TOKEN","READ");
        assertRole("WRITE FILE('F') FROM(B) RIDFLD(K)","FROM","READ");
        assertRole("WRITE FILE('F') FROM(B) RIDFLD(K)","RIDFLD","READ");
        assertRole("WRITE FILE('F') FROM(B) RIDFLD(K) RBA","RIDFLD","WRITE");
        assertRole("STARTBR FILE('F') RIDFLD(K) REQID(R)","REQID","READ");
        assertRole("INQUIRE FILE('F') DSNAME(DS) OPENSTATUS(ST)","DSNAME","WRITE");
        assertRole("SET FILE('F') DSNAME('ADMIN.ATTRIBUTE')","DSNAME","READ");
        assertRole("ENDBR FILE('F') RESP(R) RESP2(R2)","RESP","WRITE");
    }
    private void assertRole(String command,String option,String expected) {
        assertEquals(expected,parse(command).options().stream().filter(o->o.syntax().name().equals(option)).findFirst().orElseThrow().role().name());
    }
    @Test void inquireBrowseNeverReadsOldReceiverAsTargetAndAliasesAreCommandScoped() {
        var next=parse("INQUIRE FILE(NAME) NEXT OPENSTATUS(ST)");
        assertEquals("OUTPUT",next.targetMode().name());assertTrue(next.literal().isEmpty());assertTrue(next.host().isEmpty());
        assertRole("INQUIRE FILE(NAME) NEXT","FILE","WRITE");
        assertEquals("BROWSE_START",parse("INQUIRE FILE START").targetMode().name());
        assertEquals("BROWSE_END",parse("INQUIRE FILE END").targetMode().name());
        var alias=parse("SET DATASET('F') OBJECTNAME('A.B') CLOSED");
        assertEquals("F",alias.literal().orElseThrow());assertTrue(alias.gaps().isEmpty(),alias.toString());
        assertEquals("DSNAME",alias.options().get(1).canonicalName());
        assertFalse(parse("INQUIRE FILE('OLDNAME') NEXT").gaps().isEmpty());
    }
    @Test void syntaxKeepsOffsetsQualificationAndRefmodAndRejectsConflictsAndTruncation() {
        var good=parse("READ NOHANDLE FILE(NAME OF GROUP-A(3:8)) INTO(BUF) RIDFLD(KEY)");
        assertEquals("NAME OF GROUP-A(3:8)",good.host().orElseThrow());
        var option=good.options().stream().filter(o->o.canonicalName().equals("FILE")).findFirst().orElseThrow().syntax();
        assertEquals("FILE(NAME OF GROUP-A(3:8))",good.raw().substring(option.start(),option.end()));
        for(String bad:List.of("READ FILE('F') INTO(B) SET(P) RIDFLD(K)","WRITE FILE('F') INTO(B)",
            "INQUIRE FILE START NEXT","READ FILE('F') MYSTERY(B)","READ FILE('F'"))assertFalse(parse(bad).gaps().isEmpty(),bad);
    }
    @Test void otherCicsResourcesNeverBecomeFileByVerbSimilarity() {
        var parser=new CicsFileControlAnalyzer();
        for(String command:List.of("READQ TS QUEUE('Q') INTO(B)","READQ TD QUEUE('Q') INTO(B)",
            "WRITEQ TS QUEUE('Q') FROM(B)","WRITE JOURNALNAME('J') FROM(B)",
            "WRITE JOURNALNUM(1) FROM(B)","DELETE CONTAINER('C') CHANNEL('CH')", "RETURN TRANSID('T')",
            "LINK PROGRAM('P')","XCTL PROGRAM('P')","INQUIRE PROGRAM('P')","SET PROGRAM('P') NEWCOPY"))
            assertTrue(parser.parse("EXEC CICS "+command+" END-EXEC").isEmpty(),command);
    }

    @Test void typedProductSeparatesFileFromProgramControlAndBindsHostOperands() throws Exception {
        for(var entry:REQUIRED.entrySet()) {
            var state=CicsProgramControlTest.regional("01 BUF PIC X(80).\n01 WS-KEY PIC X(8).\n01 ST PIC X(4).\n01 TOK PIC X(4).",
                "EXEC CICS "+entry.getKey()+" FILE('ACCOUNTS')\n"+entry.getValue().replace("KEY)","WS-KEY)")+" NOHANDLE END-EXEC.\nCALL 'AFTER'.");
            var fact=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact)state.statements().get(0);
            assertEquals(entry.getKey(),fact.command());assertEquals("INPUT",fact.targetMode().name());
            assertEquals("LOCAL_CONDITION",fact.conditions().name());
            assertTrue(fact.ordinaryContinuation().statement().isPresent());
            assertEquals("ACCOUNTS",((io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.LiteralCallTarget)fact.target().orElseThrow()).text());
            var bytes=io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state));
            var doc=new com.fasterxml.jackson.databind.ObjectMapper().readTree(bytes);
            assertEquals("2.40.0",doc.path("contractVersion").asText());
            assertEquals("CICS_FILE_CONTROL",doc.path("statements").get(0).path("variant").asText());
            emit(entry.getKey().toLowerCase(Locale.ROOT),bytes);
        }
        var state=CicsProgramControlTest.regional("01 NAMES.\n05 FN PIC X(8).\n05 SYS PIC X(4).\n01 BUF PIC X(80).\n01 WS-KEY PIC X(8).",
            "MOVE 'ACCOUNTSR001' TO NAMES.\nEXEC CICS READ FILE(FN OF NAMES)\nSYSID(SYS OF NAMES) INTO(BUF)\nRIDFLD(WS-KEY) NOHANDLE END-EXEC.\nEXEC CICS INQUIRE FILE(FN OF NAMES)\nNEXT NOHANDLE END-EXEC.\nEXEC CICS LINK PROGRAM('PGM') NOHANDLE END-EXEC.");
        var file=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact)state.statements().get(1);
        assertTrue(((io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.DataReference)file.target().orElseThrow()).regionalAccess().isPresent());
        var sys=file.options().stream().filter(o->o.canonicalName().equals("SYSID")).findFirst().orElseThrow();
        assertTrue(sys.reference().orElseThrow().regionalAccess().isPresent());
        var output=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact)state.statements().get(2);
        assertTrue(output.target().isEmpty());assertEquals("OUTPUT",output.targetMode().name());
        assertEquals("WRITE",output.options().get(0).reference().orElseThrow().role().name());
        assertTrue(state.statements().get(3) instanceof io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact);
        emit("host-and-output",io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state)));
    }
    @Test void memoryContextAndHandlerCasesPublishTypedRolesWithoutInventedInputNames() throws Exception {
        String data="01 BUF PIC X(8).\n01 KEEP-NAME PIC X(8).\n01 FN PIC X(8).\n01 SYS PIC X(4).\n01 WS-KEY PIC X(8).\n01 ST PIC X(4).\n01 ST2 PIC X(4).\n01 WS-LEN PIC X(2).\n01 PTR USAGE POINTER.";
        var cases=new LinkedHashMap<String,String>();
        cases.put("read-call","READ FILE('ACCOUNTS') INTO(BUF) RIDFLD(WS-KEY)\nNOHANDLE");
        cases.put("write-call","WRITE FILE('ACCOUNTS') FROM(BUF) RIDFLD(WS-KEY)\nNOHANDLE");
        cases.put("length-open","READ FILE('ACCOUNTS') INTO(BUF) RIDFLD(WS-KEY)\nLENGTH(WS-LEN) NOHANDLE");
        cases.put("response","READ FILE('ACCOUNTS') INTO(BUF) RIDFLD(WS-KEY)\nRESP(ST) RESP2(ST2)");
        cases.put("set-pointer","READ FILE('ACCOUNTS') SET(PTR) RIDFLD(WS-KEY)\nNOHANDLE");
        cases.put("browse-id","STARTBR FILE('ACCOUNTS') RIDFLD(WS-KEY)\nREQID(5) SYSID('R001') NOHANDLE");
        cases.put("system-two","ENDBR FILE('ACCOUNTS') SYSID('R002') NOHANDLE");
        cases.put("inquire-next","INQUIRE FILE(FN) NEXT NOHANDLE");
        cases.put("inquire-start","INQUIRE FILE START NOHANDLE");
        cases.put("inquire-end","INQUIRE FILE END NOHANDLE");
        cases.put("set-alias","SET DATASET('ACCOUNTS') OBJECTNAME('ADMIN.ONLY')\nCLOSED NOHANDLE");
        cases.put("handler-open","READ FILE('ACCOUNTS') INTO(BUF) RIDFLD(WS-KEY)");
        cases.put("sysid-short","ENDBR FILE('ACCOUNTS') SYSID(WS-LEN) NOHANDLE");
        cases.put("file-slice","ENDBR FILE(BUF(1:8)) SYSID(SYS) NOHANDLE");
        for(var entry:cases.entrySet()) {
            String code="MOVE 'OLDNAME' TO BUF.\nMOVE 'KEEPNAME' TO KEEP-NAME.\nMOVE 'ACCOUNTS' TO FN.\nMOVE 'R001' TO SYS.\nEXEC CICS "+entry.getValue()+" END-EXEC.\nCALL BUF.\nCALL KEEP-NAME.";
            var state=CicsProgramControlTest.regional(data,code);
            var facts=state.statements().stream().filter(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact.class::isInstance).map(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact.class::cast).toList();
            assertEquals(1,facts.size(),entry.getKey());var fact=facts.get(0);
            assertEquals(entry.getKey().equals("handler-open")?"UNKNOWN":"LOCAL_CONDITION",fact.conditions().name(),entry.getKey());
            if(entry.getKey().startsWith("inquire-"))assertTrue(fact.target().isEmpty(),entry.getKey());
            if(entry.getKey().equals("set-pointer")){assertFalse(fact.options().stream().anyMatch(o->o.canonicalName().equals("INTO")));assertEquals("WRITE",fact.options().stream().filter(o->o.canonicalName().equals("SET")).findFirst().orElseThrow().role().name());}
            if(entry.getKey().equals("browse-id"))assertEquals(java.math.BigInteger.valueOf(5),fact.options().stream().filter(o->o.canonicalName().equals("REQID")).findFirst().orElseThrow().integer().orElseThrow());
            emit(entry.getKey(),io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state)));
        }
    }

    @Test void computedFileFixturesCoverFourStatesTimingAndSharedStorage() throws Exception {
        String data="01 FN PIC X(8).\n01 SYS PIC X(4).\n01 FLAG PIC X.\n01 INPUT-NAME PIC X(8).";
        var cases=new LinkedHashMap<String,String>();
        cases.put("computed-exact","MOVE 'ACCOUNTS' TO FN.\nMOVE 'R001' TO SYS.");
        cases.put("computed-closed","IF FLAG = 'Y' MOVE 'ACCOUNTS' TO FN\nELSE MOVE 'CUSTOMER' TO FN END-IF.\nMOVE 'R001' TO SYS.");
        cases.put("computed-partial","IF FLAG = 'Y' MOVE 'ACCOUNTS' TO FN\nELSE MOVE INPUT-NAME TO FN END-IF.\nMOVE 'R001' TO SYS.");
        cases.put("computed-unknown","MOVE INPUT-NAME TO FN.\nMOVE 'R001' TO SYS.");
        cases.put("computed-systems","MOVE 'ACCOUNTS' TO FN.\nIF FLAG = 'Y' MOVE 'R001' TO SYS\nELSE MOVE 'R002' TO SYS END-IF.");
        cases.put("computed-timing","MOVE 'ACCOUNTS' TO FN.\nMOVE 'R001' TO SYS.\nEXEC CICS ENDBR FILE(FN) SYSID(SYS)\nNOHANDLE END-EXEC.\nMOVE 'CUSTOMER' TO FN.\nMOVE 'R002' TO SYS.");
        for(var entry:cases.entrySet()) {
            var state=CicsProgramControlTest.regional(data,entry.getValue()+"\nEXEC CICS ENDBR FILE(FN) SYSID(SYS)\nNOHANDLE END-EXEC.");
            assertEquals(entry.getKey().equals("computed-timing")?2:1,state.statements().stream().filter(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact.class::isInstance).count());
            emit(entry.getKey(),io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state)));
        }
    }

    @Test void nonReferenceHostExpressionsDoNotConsumeUnreachableAstIdentities() {
        var state=CicsProgramControlTest.regional("01 BUF PIC X(8).\n01 WS-KEY PIC X(8).",
            "EXEC CICS READ FILE('ACCOUNTS') INTO(BUF)\nRIDFLD(WS-KEY) KEYLENGTH(LENGTH OF WS-KEY)\nLENGTH(LENGTH OF BUF) NOHANDLE END-EXEC.");
        var fact=(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact)state.statements().get(0);
        assertTrue(fact.gapCodes().contains("CICS_FILE_HOST_BINDING_UNAVAILABLE"));
        assertTrue(fact.options().stream().filter(o->o.canonicalName().equals("LENGTH")).findFirst().orElseThrow().reference().isEmpty());
    }

    @Test void legacyReadDatasetUsesFileIdentityAndPreservesSourceSpelling() {
        // C06-HUMAN-20260917: specifically READ, independently of SET aliases.
        for(String operand:List.of("'ACCOUNTS'","FN OF GROUP-A(1:8)")) {
            var parsed=new CicsFileControlAnalyzer().parse("EXEC CICS READ DATASET("+operand+") INTO(B) RIDFLD(K) NOHANDLE END-EXEC");
            assertTrue(parsed.isPresent(),"legacy READ DATASET must be an observed CICS FILE");
            var fact=parsed.orElseThrow();var option=fact.options().get(0);
            assertEquals("READ",fact.command().name());assertEquals("INPUT",fact.targetMode().name());
            assertEquals("FILE",option.canonicalName());assertEquals("DATASET",option.syntax().name());
            assertEquals("READ",option.role().name());assertTrue(fact.gaps().isEmpty(),fact.toString());
            assertEquals("DATASET("+operand+")",fact.raw().substring(option.syntax().start(),option.syntax().end()));
            if(operand.startsWith("'"))assertEquals("ACCOUNTS",fact.literal().orElseThrow());
            else assertEquals(operand,fact.host().orElseThrow());
        }
    }
    @Test void legacyReadDatasetDoesNotGeneralizeOrHideMalformedIdentity() {
        var analyzer=new CicsFileControlAnalyzer();
        for(String command:REQUIRED.keySet())if(!Set.of("READ","SET").contains(command))
            assertTrue(analyzer.parse("EXEC CICS "+command+" DATASET('F') "+REQUIRED.get(command)+" END-EXEC").isEmpty(),command);
        for(String bad:List.of("READ DATASET('F') FILE('G') INTO(B) RIDFLD(K)",
            "READ DATASET INTO(B) RIDFLD(K)","READ DATASET() INTO(B) RIDFLD(K)",
            "READ DATASET('F') INTO(B) SET(P) RIDFLD(K)","READ DATASET('F'")) {
            var parsed=analyzer.parse("EXEC CICS "+bad+" END-EXEC");
            assertTrue(parsed.isPresent(),"malformed authorized alias remains observed");
            assertFalse(parsed.orElseThrow().gaps().isEmpty(),bad);
            if(!bad.contains("SET(P)"))assertTrue(parsed.orElseThrow().literal().isEmpty(),bad);
        }
    }
    @Test void legacyReadDatasetPublishesCanonicalContractAndOriginalOption() throws Exception {
        String data="01 FN PIC X(8).\n01 BUF PIC X(8).\n01 WS-KEY PIC X(8).";
        for(String operand:List.of("'ACCOUNTS'","FN")) {
            var state=CicsProgramControlTest.regional(data,"MOVE 'ACCOUNTS' TO FN.\nEXEC CICS READ DATASET("+operand+")\nINTO(BUF) RIDFLD(WS-KEY) NOHANDLE END-EXEC.\nEXEC CICS LINK PROGRAM('PGM') NOHANDLE END-EXEC.\nCALL 'AFTER'.");
            var facts=state.statements().stream().filter(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact.class::isInstance).map(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFileFact.class::cast).toList();
            assertEquals(1,facts.size());var fact=facts.get(0);var option=fact.options().get(0);
            assertEquals("READ",fact.command());assertEquals("cics-ts.file@1",fact.nameProfile());
            assertEquals("FILE",option.canonicalName());assertEquals("DATASET",option.name());
            assertTrue(fact.rawText().substring(option.start(),option.end()).startsWith("DATASET("));
            assertEquals("READ",option.role().name());assertTrue(fact.target().isPresent());
            assertTrue(state.statements().stream().anyMatch(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticProduct.CicsFact.class::isInstance));
            var bytes=io.github.gustavo2358.cobolexplorer.semanticproduct.transport.SemanticProductJsonWriter.serialize(io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort.open(state));
            emit(operand.equals("FN")?"read-dataset-computed":"read-dataset-literal",bytes);
        }
    }

    static void emit(String name,byte[] bytes)throws Exception {
        String output=System.getProperty("cics.file.fixtures.dir");if(output==null)return;
        var dir=java.nio.file.Path.of(output);java.nio.file.Files.createDirectories(dir);java.nio.file.Files.write(dir.resolve(name+".sp.json"),bytes);
    }
}
