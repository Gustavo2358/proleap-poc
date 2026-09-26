package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FixedTabNormalizationTest {
    private SourceNormalizer.Result normalize(String raw) {
        return SourceNormalizer.normalize(raw, "input.cbl", SourceNormalizer.SourceFormat.FIXED);
    }

    @Test void advancesToNextFourColumnStopRatherThanAddingFixedWidth() {
        assertEquals("  05 FIELD PIC X.\n", normalize("\t     05 FIELD PIC X.\n").text());
        assertEquals("      05 FIELD PIC X.\n", normalize("\t\t     05 FIELD PIC X.\n").text());
        assertEquals(" "+"DISPLAY 'A'.", normalize("      \tDISPLAY 'A'.").text());
        assertEquals(" "+"DISPLAY 'A'.", normalize("       \tDISPLAY 'A'.").text());
        assertEquals(" ".repeat(13)+"((:FLAG = '1'\n", normalize("181300           \t((:FLAG = '1'\n").text());
    }

    @Test void retainsLongDeclarationAfterTwoIndentationTabs() {
        String raw="\t\t     05  CUSTOMER-FIELD                          PIC X(25).\n";
        assertTrue(normalize(raw).text().endsWith("PIC X(25).\n"));
        assertEquals(normalize(raw.replace("\t\t", "        ")).text(), normalize(raw).text());
        assertThrows(IllegalArgumentException.class, () -> normalize("\tCOPY MEMBER.\n"),
                "A four-column tab does not silently move text out of an invalid indicator");
    }

    @Test void preservesLiteralAndCommentPayloadAndLineEndings() {
        for (String nl : new String[]{"\n", "\r\n", "\r"}) {
            assertEquals(" DISPLAY 'A\tB''C\tD'. *> keep\tpayload"+nl,
                    normalize("       \tDISPLAY 'A\tB''C\tD'. *> keep\tpayload"+nl).text());
            assertEquals("*>  keep\tpayload"+nl, normalize("      * keep\tpayload"+nl).text());
            assertEquals("DISPLAY 'A\tB'."+nl+nl,
                    normalize("       DISPLAY 'A\t"+nl+"      -    'B'."+nl).text());
        }
    }

    @Test void retainsIndicatorAndColumn72Boundary() {
        assertEquals("*>  PAGE\tTEXT\n", normalize("      / PAGE\tTEXT\n").text());
        assertThrows(IllegalArgumentException.class, () -> normalize("      ?BAD\n"));
        assertEquals("A".repeat(57)+" ".repeat(4)+"X", normalize("       "+"A".repeat(57)+"\tX").text());
        assertEquals("A".repeat(64)+" ", normalize("       "+"A".repeat(64)+"\tX").text());
        assertEquals("A".repeat(65), normalize("       "+"A".repeat(65)+"\tX").text());
    }

    @Test void mapsExpandedSpacesToTabAndFollowingTokensExactly() {
        String raw="       \tDISPLAY '😀\tX'.\r\n";
        var r=normalize(raw);
        var tab=r.sourceMap().provenance(0,1);
        assertEquals(7,tab.original().startColumn()); assertEquals(7,tab.original().endColumn());
        assertFalse(tab.exact());
        var token=r.sourceMap().provenance(1,8);
        assertEquals(8,token.original().startColumn()); assertEquals(14,token.original().endColumn());
        assertTrue(token.exact());
        int newline=new UnicodeText(r.text()).indexOf("\r\n",0);
        assertTrue(r.sourceMap().provenance(newline,newline+2).exact());
        var prefix=normalize("\t     05 FIELD PIC X.\n");
        int field=prefix.text().indexOf("FIELD");
        assertEquals(9,prefix.sourceMap().provenance(field,field+5).original().startColumn());
        assertTrue(prefix.sourceMap().provenance(field,field+5).exact());
    }

    @Test void usesIdenticalPolicyForNestedCopyAndRetainsIncludeChain(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("ONE.cpy"),"\t\tCOPY TWO.\n");
        Files.writeString(dir.resolve("TWO.cpy"),"\t     05 FIELD PIC X.\n");
        var out=new PreprocessorEngine(Bindings.cobol(),new CopybookLibrary(dir))
                .process(normalize("\t\tCOPY ONE.\n").sourceMap(),"input.cbl");
        assertEquals(0,out.errors()); assertEquals(0,out.unresolved());
        int field=out.text().indexOf("FIELD"); assertTrue(field>=0);
        var provenance=out.sourceMap().provenance(field,field+5);
        assertEquals("TWO.cpy",provenance.original().file());
        assertEquals(9,provenance.original().startColumn()); assertEquals(2,provenance.includeChain().size());
        assertTrue(provenance.exact());
    }
}
