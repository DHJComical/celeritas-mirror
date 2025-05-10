package net.irisshaders.iris.gl.state;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ShaderAttributeInputsBuilder {
    private boolean color;
    private boolean tex;
    private boolean overlay;
    private boolean light;
    private boolean normal;
    private boolean newLines;
    private boolean glint;
    private boolean text;

    public ShaderAttributeInputsBuilder(VertexFormat format, boolean isFullbright, boolean isLines, boolean glint, boolean text) {
        if (format == DefaultVertexFormat.POSITION_COLOR_NORMAL && !isLines) {
            newLines = true;
        }

        this.text = text;
        this.glint = glint;

        format.getElementAttributeNames().forEach(name -> {
            if ("Color".equals(name)) {
                color = true;
            }

            if ("UV0".equals(name)) {
                tex = true;
            }

            if ("UV1".equals(name)) {
                overlay = true;
            }

            if ("UV2".equals(name) && !isFullbright) {
                light = true;
            }

            if ("Normal".equals(name)) {
                normal = true;
            }
        });
    }

    public ShaderAttributeInputs build() {
        return new ShaderAttributeInputs(color, tex, overlay, light, normal, newLines, glint, text);
    }
}
