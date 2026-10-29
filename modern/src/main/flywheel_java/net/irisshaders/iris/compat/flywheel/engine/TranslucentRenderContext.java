package net.irisshaders.iris.compat.flywheel.engine;

//? if <1.21
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.backend.RenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record TranslucentRenderContext(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                       /*? if >=1.21 {*/ /*Matrix4fc modelView, *//*?} else {*/ PoseStack stack, /*?}*/ Matrix4fc projection, Matrix4fc viewProjection,
                                       Camera camera, float partialTick) implements RenderContext
{
    public static TranslucentRenderContext create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                                  /*? if >=1.21 {*/ /*Matrix4fc modelView, *//*?} else {*/ PoseStack stack, /*?}*/ Matrix4f projection,
                                                  Camera camera, float partialTick)
    {
        Matrix4f viewProjection = new Matrix4f(projection);
        viewProjection.mul(/*? if >=1.21 {*/ /*modelView *//*?} else {*/ stack.last().pose() /*?}*/);

        return new TranslucentRenderContext(renderer, level, buffers, /*? if >=1.21 {*/ /*modelView *//*?} else {*/ stack /*?}*/, projection, viewProjection, camera, partialTick);
    }
}
