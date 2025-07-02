package org.embeddedt.embeddium.compat.mc;

import java.io.IOException;
import java.nio.file.Path;

public interface MCDumpable {
    void dumpContents(MCResourceLocation resourceLocation, Path path) throws IOException;
}
