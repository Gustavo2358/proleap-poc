package io.proleap.benchmark;

import java.io.IOException;
import java.nio.file.Path;

public final class Cobol85Frontend extends AntlrCobolFrontend {
    public Cobol85Frontend(Path copybooks) throws IOException { super(Bindings.cobol85(), copybooks); }
}
