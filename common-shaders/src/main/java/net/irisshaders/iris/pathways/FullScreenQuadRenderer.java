package net.irisshaders.iris.pathways;

import java.util.ServiceLoader;

public interface FullScreenQuadRenderer {

    FullScreenQuadRenderer INSTANCE = ServiceLoader.load(FullScreenQuadRenderer.class).findFirst().orElseThrow();

    void render();

    void begin();

    void renderQuad();

    void end();

}
