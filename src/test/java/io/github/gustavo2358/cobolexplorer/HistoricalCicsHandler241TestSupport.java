package io.github.gustavo2358.cobolexplorer;

import java.util.Optional;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CobolSemanticPort;
import io.github.gustavo2358.cobolexplorer.semanticproduct.projection.CobolSemanticProductProjector;

/** Frozen 2.41 producer composition, before the explicitly versioned ABEND capability.
 * Does not downgrade a 2.42 product. Historical semantic assertions remain unchanged. */
final class HistoricalCicsHandler241TestSupport {
    static CobolSemanticPort publish(AstBoundaryTestSupport.Analysis a,int index,StorageLayoutSemantics.Profile profile) {
        return publish(a,index,profile,StorageInitialSemantics.EntryMode.UNKNOWN);
    }
    static CobolSemanticPort publish(AstBoundaryTestSupport.Analysis a,int index,StorageLayoutSemantics.Profile profile,StorageInitialSemantics.EntryMode mode) {
        var frontend=a.build();var tables=a.tables();var resolution=a.resolution();var report=a.report();
        var components=StorageComponents.analyze(frontend,tables,resolution);
        var layout=StorageLayoutSemantics.analyze(frontend,tables,resolution,report,profile,components,false);
        var cics=new CicsProgramControlAnalyzer().analyze(frontend,report,CicsProgramControlAnalyzer.EntryMode.UNKNOWN)
            .withHandlers(CicsHandlerSemantics.analyze(frontend,tables,resolution));
        var storage=StorageAccessSemantics.analyze(frontend,resolution,layout,mode,cics);
        var products=new CobolSemanticProductProjector.FrontendProducts(frontend,tables,a.occurrences(),resolution,report,
            ScalarMoveSemantics.analyze(frontend,tables,resolution,report,components,Optional.of(storage),cics),Optional.of(storage),Optional.of(cics));
        return CobolSemanticProductProjector.open(products,a.model().programUnits().get(index).id());
    }
}
