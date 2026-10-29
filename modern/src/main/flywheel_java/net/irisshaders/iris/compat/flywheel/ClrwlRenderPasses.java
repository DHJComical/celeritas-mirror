package net.irisshaders.iris.compat.flywheel;

//? if <1.21
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.flywheel.engine.ShadowRenderContext;
import net.irisshaders.iris.compat.flywheel.engine.ShadowRenderingPhase;
import net.irisshaders.iris.compat.flywheel.engine.TranslucentRenderContext;
import net.irisshaders.iris.pipeline.ShaderRenderPassEvent;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/**
 * Draws Flywheel content in the shadow and translucent passes. Flywheel only calls engines once per frame, after
 * entities; the extra passes go through the same dispatcher so they wait for the frame plan like that call does.
 */
public final class ClrwlRenderPasses
{
    private ClrwlRenderPasses()
    {
    }

    public static void init()
    {
        ShaderRenderPassEvent.BUS.addListener(ClrwlRenderPasses::onRenderPass);
    }

    private static void onRenderPass(ShaderRenderPassEvent event)
    {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;

        if (level == null || !ClrwlBackend.isCurrentBackend(BackendManager.currentBackend()))
        {
            return;
        }

        var manager = VisualizationManager.get(level);

        if (manager == null)
        {
            return;
        }

        //? if <1.21 {
        var modelView = new PoseStack();
        modelView.last().pose().set(event.getModelView());
        //?} else
        //var modelView = event.getModelView();
        var projection = new Matrix4f(event.getProjection());

        RenderContext context = switch (event.getStage())
        {
            case TRANSLUCENT -> TranslucentRenderContext.create(minecraft.levelRenderer, level, minecraft.renderBuffers(),
                    modelView, projection, event.getCamera(), event.getPartialTick());
            case SHADOW_SOLID, SHADOW_TRANSLUCENT ->
            {
                if (!shouldRenderShadow())
                {
                    yield null;
                }

                var cameraPos = CameraUniforms.getUnshiftedCameraPosition();

                yield ShadowRenderContext.create(minecraft.levelRenderer, level, minecraft.renderBuffers(),
                        modelView, projection,
                        CapturedRenderingState.INSTANCE.getGbufferModelView(), CapturedRenderingState.INSTANCE.getGbufferProjection(),
                        event.getCamera(), (float) cameraPos.x(), (float) cameraPos.y(), (float) cameraPos.z(),
                        event.getPartialTick(),
                        event.getStage() == ShaderRenderPassEvent.Stage.SHADOW_SOLID ? ShadowRenderingPhase.SOLID : ShadowRenderingPhase.TRANSLUCENT);
            }
        };

        if (context != null)
        {
            manager.renderDispatcher().afterEntities(context);
        }
    }

    private static boolean shouldRenderShadow()
    {
        var pack = Iris.getCurrentPack();

        if (pack.isEmpty())
        {
            return false;
        }

        var programs = pack.get().getProgramSet(Iris.getCurrentDimension()).getClrwlPrograms();
        return programs != null && programs.getClrwlDirectives().shouldRenderShadow();
    }
}
