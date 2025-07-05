package net.irisshaders.iris.pipeline.programs;

import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import org.embeddedt.embeddium.compat.mc.MCVertexFormat;

public interface ShaderKey {
    int ordinal();
    ProgramId getProgram();
    AlphaTest getAlphaTest();
    MCVertexFormat getVertexFormat();
    FogMode getFogMode();
    boolean isIntensity();
    String getName();
    boolean isShadow();
    boolean hasDiffuseLighting();
    boolean shouldIgnoreLightmap();
    boolean isGlint();
    boolean isText();
}
