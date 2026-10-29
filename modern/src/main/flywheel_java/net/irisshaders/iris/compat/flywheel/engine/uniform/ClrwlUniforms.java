package net.irisshaders.iris.compat.flywheel.engine.uniform;

import net.irisshaders.iris.compat.flywheel.compile.ClrwlProgram;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;

public class ClrwlUniforms
{
    public static final int FRAME_INDEX = 0;
    public static final int PASS_INDEX = 1;
    public static final int LEVEL_INDEX = 2;
    public static final int PLAYER_INDEX = 3;
    public static final int FOG_INDEX = 4;
    public static final int OPTIONS_INDEX = 5;

    public static final String FRAME_BLOCK_NAME = "_ClrwlFrameUniforms";
    public static final String PASS_BLOCK_NAME = "_ClrwlPassUniforms";
    public static final String LEVEL_BLOCK_NAME = "_ClrwlLevelUniforms";
    public static final String PLAYER_BLOCK_NAME = "_ClrwlPlayerUniforms";
    public static final String FOG_BLOCK_NAME = "_ClrwlFogUniforms";
    public static final String OPTIONS_BLOCK_NAME = "_ClrwlOptionsUniforms";

    public static void updateFrame(RenderContext context)
    {
        ClrwlFrameUniforms.update(context);
        ClrwlLevelUniforms.update(context);
        ClrwlPlayerUniforms.update(context);
        ClrwlFogUniforms.update();
    }

    public static void bind(boolean isShadow)
    {
        if (isShadow)
        {
            bindShadow();
        }
        else
        {
            bindGbuffers();
        }
    }

    public static void bindGbuffers()
    {
        ClrwlFrameUniforms.BUFFER.bind();
        ClrwlGbuffersPassUniforms.BUFFER.bind();
        ClrwlLevelUniforms.BUFFER.bind();
        ClrwlPlayerUniforms.BUFFER.bind();
        ClrwlFogUniforms.BUFFER.bind();
        ClrwlOptionsUniforms.BUFFER.bind();
    }

    public static void bindShadow()
    {
        ClrwlFrameUniforms.BUFFER.bind();
        ClrwlShadowPassUniforms.BUFFER.bind();
        ClrwlLevelUniforms.BUFFER.bind();
        ClrwlPlayerUniforms.BUFFER.bind();
        ClrwlFogUniforms.BUFFER.bind();
        ClrwlOptionsUniforms.BUFFER.bind();
    }

    public static void setUniformsBlockBindings(GlProgram program)
    {
        program.setUniformBlockBinding(FRAME_BLOCK_NAME, FRAME_INDEX);
        program.setUniformBlockBinding(PASS_BLOCK_NAME, PASS_INDEX);
        program.setUniformBlockBinding(LEVEL_BLOCK_NAME, LEVEL_INDEX);
        program.setUniformBlockBinding(PLAYER_BLOCK_NAME, PLAYER_INDEX);
        program.setUniformBlockBinding(FOG_BLOCK_NAME, FOG_INDEX);
        program.setUniformBlockBinding(OPTIONS_BLOCK_NAME, OPTIONS_INDEX);
    }

    public static void setUniformsBlockBindings(ClrwlProgram program)
    {
        program.setUniformBlockBinding(FRAME_BLOCK_NAME, FRAME_INDEX);
        program.setUniformBlockBinding(PASS_BLOCK_NAME, PASS_INDEX);
        program.setUniformBlockBinding(LEVEL_BLOCK_NAME, LEVEL_INDEX);
        program.setUniformBlockBinding(PLAYER_BLOCK_NAME, PLAYER_INDEX);
        program.setUniformBlockBinding(FOG_BLOCK_NAME, FOG_INDEX);
        program.setUniformBlockBinding(OPTIONS_BLOCK_NAME, OPTIONS_INDEX);
    }
}
