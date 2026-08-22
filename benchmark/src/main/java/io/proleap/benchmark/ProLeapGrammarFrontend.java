package io.proleap.benchmark;

import java.io.IOException;
import java.nio.file.Path;

public final class ProLeapGrammarFrontend extends AntlrCobolFrontend {
    public ProLeapGrammarFrontend(Path copybooks) throws IOException { super(Bindings.proleap(), copybooks); }
}
