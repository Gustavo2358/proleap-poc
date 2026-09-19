package io.github.gustavo2358.cobolexplorer;
import java.util.*;
/** Isolates extraction from JVM startup, source preprocessing and serialization. */
public final class Db2ExtractionScaleProbe {
    public static void main(String[] args) {
        int n=Integer.parseInt(args[0]),unique=Integer.parseInt(args[1]);var sql=new ArrayList<String>();
        for(int i=0;i<n;i++)sql.add("SELECT * FROM T"+String.format(Locale.ROOT,"%05d",i%unique));
        long[] samples=new long[11];
        for(int run=0;run<16;run++) {long start=System.nanoTime();int occurrences=0;for(var s:sql){var r=Db2SourceExtractor.extract(s);if(!r.gaps().isEmpty())throw new AssertionError(r);occurrences+=r.tables().size();}long elapsed=System.nanoTime()-start;if(occurrences!=n)throw new AssertionError();if(run>=5)samples[run-5]=elapsed;}
        Arrays.sort(samples);System.out.println("{\"extractionNanosMedian\":"+samples[5]+"}");
    }
}
