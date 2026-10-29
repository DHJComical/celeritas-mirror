package net.irisshaders.iris.compat.flywheel.compile.transform;

import dev.engine_room.flywheel.backend.gl.GlCompat;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

public class ClrwlComputeTransformPatcher
{
    public static String patchCompute(String shader, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
    {
        var parameters = new ClrwlTransformParameters(ShaderType.COMPUTE, false, false, textureMap);
        var t = ClrwlTransformPatcher.parse(shader, GlCompat.MAX_GLSL_VERSION.version);

        ClrwlTransformPatcher.replaceMatrices(t);
        ClrwlTransformPatcher.finish(t, parameters);

        return ClrwlTransformPatcher.print(t);
    }
}
