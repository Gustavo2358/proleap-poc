package io.proleap.benchmark;

import java.nio.file.Path;

public interface CobolFrontend {
    String name();
    FrontendResult parse(Path source);
}
