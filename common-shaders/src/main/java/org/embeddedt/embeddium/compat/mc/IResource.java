package org.embeddedt.embeddium.compat.mc;

import java.io.IOException;
import java.io.InputStream;

public interface IResource {
    InputStream open() throws IOException;
}
