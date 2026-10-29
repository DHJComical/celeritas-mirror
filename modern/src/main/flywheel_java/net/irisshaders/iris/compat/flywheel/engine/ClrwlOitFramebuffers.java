package net.irisshaders.iris.compat.flywheel.engine;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.compat.flywheel.ClrwlSamplers;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlOitPrograms;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlOitConfig;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import net.irisshaders.iris.compat.flywheel.util.Utils;
import dev.engine_room.flywheel.backend.NoiseTextures;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.GlTextureUnit;
import dev.engine_room.flywheel.lib.math.MoreMath;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.BlendModeFunction;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shadows.ShadowMatrices;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL46;

import java.util.List;

public class ClrwlOitFramebuffers
{
    public static final float[] CLEAR_TO_ZERO = {0, 0, 0, 0};
    public static final int[] DEPTH_RANGE_DRAW_BUFFERS = {GL46.GL_COLOR_ATTACHMENT0};
    public static final int[] DEPTH_ONLY_DRAW_BUFFERS = {};

    private final int[] programDrawBuffers;

    private int[] renderTransmittanceDrawBuffers;
    private int[] accumulateDrawBuffers;

    private final ClrwlProgramGroup programGroup;
    private final ClrwlOitPrograms programs;
    private final IrisRenderingPipeline irisPipeline;
    private final ClrwlOitConfig config;

    private final float passZFar;

    private final int vao;

    public int mainFbo = -1;
    public int coeffsFbo = -1;

    public int depthBounds = -1;
    public int[] coefficients;
    public int[] accumulate;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public ClrwlOitFramebuffers(ClrwlProgramGroup programGroup, ClrwlOitPrograms programs, IrisRenderingPipeline irisPipeline, ProgramSet programSet, ProgramDirectives directives)
    {
        ClrwlOitConfig config = programSet.getClrwlPrograms().getClrwlDirectives().getOitConfig(programGroup);
        PackDirectives packDirectives = programSet.getPackDirectives();

        this.programGroup = programGroup;
        this.programs = programs;
        this.irisPipeline = irisPipeline;
        this.config = config;
        this.programDrawBuffers = directives.getDrawBuffers();

        if (GlCompat.SUPPORTS_DSA)
        {
            vao = GL46.glCreateVertexArrays();
        }
        else
        {
            vao = GL32.glGenVertexArrays();
        }

        switch (programGroup)
        {
            case GBUFFERS ->
            {
                passZFar = Minecraft.getInstance().gameRenderer.getDepthFar();
            }

            case SHADOW ->
            {
                var shadowDirectives = packDirectives.getShadowDirectives();

                if (shadowDirectives.getFov() != null)
                {
                    passZFar = ShadowMatrices.FAR;
                }
                else
                {
                    passZFar = shadowDirectives.getFarPlane();
                }
            }

            default ->
            {
                passZFar = 4242.0f;
            }
        }
    }

    public void delete()
    {
        deleteTextures();
        GL32.glDeleteVertexArrays(vao);
    }

    private void deleteTextures()
    {
        // We sometimes get the same texture ID back when creating new textures,
        // so bind zero to clear the GlStateManager

        if (depthBounds != -1)
        {
            GL32.glDeleteTextures(depthBounds);
            depthBounds = -1;
        }

        if (coefficients != null)
        {
            for (int i = 0; i < coefficients.length; i++)
            {
                ClrwlSamplers.getCoefficient(i).makeActive();
                RenderSystem.bindTexture(0);
            }

            GL32.glDeleteTextures(coefficients);
            coefficients = null;
        }

        if (accumulate != null)
        {
            GL32.glDeleteTextures(accumulate);
            accumulate = null;
        }

        if (mainFbo != -1)
        {
            GL32.glDeleteFramebuffers(mainFbo);
            mainFbo = -1;
        }

        if (coeffsFbo != -1)
        {
            GL32.glDeleteFramebuffers(coeffsFbo);
            coeffsFbo = -1;
        }

        ClrwlSamplers.DEPTH_RANGE.makeActive();
        RenderSystem.bindTexture(0);
    }

    /**
     * Set up the framebuffer.
     */
    public void prepare()
    {
        var depthInfo = ProgramGroupDepthInfo.of(irisPipeline, programGroup);

        maybeResizeFBOS(depthInfo.width(), depthInfo.height());

        if (this.coeffsFbo != -1)
        {
            for (int i = 0; i < coefficients.length; i++)
            {
                int buffer = coefficients[i];

                ClrwlSamplers.getCoefficient(i).makeActive();

                // Bind zero to render system to make sure we clear their internal state
                RenderSystem.bindTexture(0);
                GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, buffer);
            }
        }

        ClrwlSamplers.DEPTH_RANGE.makeActive();
        RenderSystem.bindTexture(depthBounds);

        ClrwlSamplers.NOISE.makeActive();
        NoiseTextures.BLUE_NOISE.bind();

        if (coeffsFbo != -1)
        {
            GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, coeffsFbo);
            GL32.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, depthInfo.textureId(), 0);
        }

        GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, mainFbo);
        GL32.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, depthInfo.textureId(), 0);
    }

    /**
     * Render out the min and max depth per fragment.
     */
    public void prepareDepthRange()
    {
        // No depth writes, but we'll still use the depth test.
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL32.GL_MAX);

        if (GlCompat.SUPPORTS_DSA)
        {
            GL46.glNamedFramebufferDrawBuffers(mainFbo, DEPTH_RANGE_DRAW_BUFFERS);
            GL46.glClearNamedFramebufferfv(mainFbo, GL46.GL_COLOR, 0, new float[]{-passZFar, -passZFar, 0, 0});
        }
        else
        {
            GL32.glDrawBuffers(DEPTH_RANGE_DRAW_BUFFERS);
            RenderSystem.clearColor(-passZFar, -passZFar, 0, 0);
            RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT, false);
        }
    }

    /**
     * Generate the coefficients to the transmittance function.
     */
    public boolean prepareRenderTransmittance()
    {
        if (coeffsFbo == -1) // Only frontmost fragments
        {
            return false;
        }

        GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, coeffsFbo);

        // No depth writes, but we'll still use the depth test
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL32.GL_FUNC_ADD);

        if (GlCompat.SUPPORTS_DSA)
        {
            GL46.glNamedFramebufferDrawBuffers(coeffsFbo, renderTransmittanceDrawBuffers);

            for (int i = 0; i < renderTransmittanceDrawBuffers.length; i++)
            {
                GL46.glClearNamedFramebufferfv(coeffsFbo, GL46.GL_COLOR, i, CLEAR_TO_ZERO);
            }
        }
        else
        {
            GL32.glDrawBuffers(renderTransmittanceDrawBuffers);
            RenderSystem.clearColor(0, 0, 0, 0);
            RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT, false);
        }

        return true;
    }

    /**
     * If any fragment has its transmittance fall off to zero, search the transmittance
     * function to determine at what depth that occurs and write out to the depth buffer.
     */
//    public void renderDepthFromTransmittance()
//    {
//        if (coeffsFbo == -1) // Only opaques
//        {
//            return;
//        }
//
//        // Only write to depth, not color.
//        RenderSystem.depthMask(true);
//        RenderSystem.colorMask(false, false, false, false);
//        RenderSystem.disableBlend();
//        RenderSystem.depthFunc(GL32.GL_ALWAYS);
//
//        if (GlCompat.SUPPORTS_DSA)
//        {
//            GL46.glNamedFramebufferDrawBuffers(coeffsFbo, DEPTH_ONLY_DRAW_BUFFERS);
//        }
//        else
//        {
//            GL32.glDrawBuffers(DEPTH_ONLY_DRAW_BUFFERS);
//        }
//
//        programs.getOitDepthProgram()
//                .bind();
//
//        drawFullscreenQuad();
//    }

    /**
     * Sample the transmittance function and accumulate.
     */
    public void prepareAccumulate()
    {
        GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, mainFbo);

        // No depth writes, but we'll still use the depth test
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL32.GL_FUNC_ADD);

        if (GlCompat.SUPPORTS_DSA)
        {
            GL46.glNamedFramebufferDrawBuffers(mainFbo, accumulateDrawBuffers);

            for (int i = 0; i < accumulateDrawBuffers.length; i++)
            {
                GL46.glClearNamedFramebufferfv(mainFbo, GL46.GL_COLOR, i, CLEAR_TO_ZERO);
            }
        }
        else
        {
            GL32.glDrawBuffers(accumulateDrawBuffers);
            RenderSystem.clearColor(0, 0, 0, 0);
            RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT, false);
        }
    }

    /**
     * Composite the accumulated luminance onto the main framebuffer.
     */
    public void composite(GlFramebuffer target, @Nullable ClrwlBlendModeOverride blendModeOverride, List<BufferBlendInformation> bufferBlendOverrides, boolean isShadow)
    {
        target.bind();

        // The composite shader writes out the closest depth to gl_FragDepth.
        // depthMask = true: OIT stuff renders on top of other transparent stuff.
        // depthMask = false: other transparent stuff renders on top of OIT stuff.
        // If Neo gets wavelet OIT we can use their hooks to be correct with everything.
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);

        BlendMode blendMode;

        if (blendModeOverride == null)
        {
            // We rely on the blend func to achieve:
            // final color = (1 - transmittance_total) * sum(color_f * alpha_f * transmittance_f) / sum(alpha_f * transmittance_f)
            //			+ color_dst * transmittance_total
            //
            // Though note that the alpha value we emit in the fragment shader is actually (1. - transmittance_total).
            // The extra inversion step is so we can have a sane alpha value written out for the fabulous blit shader to consume.

            blendMode = new BlendMode(
                BlendModeFunction.SRC_ALPHA.getGlId(),
                BlendModeFunction.ONE_MINUS_SRC_ALPHA.getGlId(),
                BlendModeFunction.ONE.getGlId(),
                BlendModeFunction.ONE_MINUS_SRC_ALPHA.getGlId()
            );
        }
        else
        {
            blendMode = blendModeOverride.blendMode();
        }

        if (blendMode == null)
        {
            RenderSystem.disableBlend();
        }
        else
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(blendMode.srcRgb(), blendMode.dstRgb(), blendMode.srcAlpha(), blendMode.dstAlpha());
            RenderSystem.blendEquation(GL32.GL_FUNC_ADD);
        }

        for (var entry : bufferBlendOverrides)
        {
            if (entry.blendMode() == null)
            {
                IrisRenderSystem.disableBufferBlend(entry.index());
            }
            else
            {
                IrisRenderSystem.enableBufferBlend(entry.index());
                IrisRenderSystem.blendFuncSeparatei(entry.index(), entry.blendMode().srcRgb(), entry.blendMode().dstRgb(), entry.blendMode().srcAlpha(), entry.blendMode().dstAlpha());
            }
        }

        RenderSystem.depthFunc(GL32.GL_ALWAYS);

        for (int i = 0; i < accumulate.length; i++)
        {
            ClrwlSamplers.getAccumulate(i).makeActive();
            RenderSystem.bindTexture(accumulate[i]);
        }

        programs.getOitCompositeProgram(programDrawBuffers, config.coeffRanks(), config.accumulateOverrides(), isShadow)
                .bind();

        drawFullscreenQuad();
    }

    private void drawFullscreenQuad()
    {
        // Empty VAO, the actual full screen triangle is generated in the vertex shader
        GlStateManager._glBindVertexArray(vao);
        GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, 3);
    }

    private void maybeResizeFBOS(int width, int height)
    {
        if (lastWidth == width && lastHeight == height)
        {
            return;
        }

        lastWidth = width;
        lastHeight = height;

        deleteTextures();

        resizeMainFBO(width, height);
        resizeCoeffsFBO(width, height);
    }

    private void resizeMainFBO(int width, int height)
    {
        var overrides = config.accumulateOverrides();
        var accumulateCnt = programDrawBuffers.length;

        mainFbo = GlCompat.SUPPORTS_DSA ? GL46.glCreateFramebuffers() : GL32.glGenFramebuffers();
        accumulate = new int[accumulateCnt];
        accumulateDrawBuffers = new int[accumulateCnt];

        if (GlCompat.SUPPORTS_DSA)
        {
            depthBounds = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);

            GL46.glTextureStorage2D(depthBounds, 1, GL32.GL_RG32F, width, height);

            for (int i = 0; i < accumulateCnt; i++)
            {
                var drawBuffer = programDrawBuffers[i];
                var format = InternalTextureFormat.RGBA16F;

                var maybeOverride = Utils.findFirst(overrides, e -> e.drawBuffer() == drawBuffer);

                if (maybeOverride.isPresent())
                {
                    format  = maybeOverride.get().format();
                }

                int buffer = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);

                GL46.glTextureStorage2D(buffer, 1, format.getGlFormat(), width, height);
                accumulate[i] = buffer;
            }

            GL46.glNamedFramebufferTexture(mainFbo, GL32.GL_COLOR_ATTACHMENT0, depthBounds, 0);

            for (int i = 0; i < accumulateCnt; i++)
            {
                GL46.glNamedFramebufferTexture(mainFbo, GL32.GL_COLOR_ATTACHMENT1 + i, accumulate[i], 0);
                accumulateDrawBuffers[i] = GL32.GL_COLOR_ATTACHMENT1 + i;
            }
        }
        else
        {
            depthBounds = GL32.glGenTextures();

            GlTextureUnit.T0.makeActive();
            RenderSystem.bindTexture(0);

            GL32.glBindTexture(GL32.GL_TEXTURE_2D, depthBounds);
            GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RG32F, width, height, 0, GL46.GL_RGBA, GL46.GL_BYTE, 0);

            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);

            for (int i = 0; i < accumulateCnt; i++)
            {
                var drawBuffer = programDrawBuffers[i];
                var format = InternalTextureFormat.RGBA16F;

                var maybeOverride = Utils.findFirst(overrides, e -> e.drawBuffer() == drawBuffer);

                if (maybeOverride.isPresent())
                {
                    format  = maybeOverride.get().format();
                }

                int buffer = GL32.glGenTextures();

                GL32.glBindTexture(GL32.GL_TEXTURE_2D, buffer);
                GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, format.getGlFormat(), width, height, 0, format.getPixelFormat().getGlFormat(), GL32.GL_UNSIGNED_BYTE, 0);

                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);

                accumulate[i] = buffer;
            }

            GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, mainFbo);

            GL46.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, depthBounds, 0);

            for (int i = 0; i < accumulateCnt; i++)
            {
                GL46.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT1 + i, accumulate[i], 0);
                accumulateDrawBuffers[i] = GL32.GL_COLOR_ATTACHMENT1 + i;
            }
        }
    }

    private void resizeCoeffsFBO(int width, int height)
    {
        var coeffRanks = config.coeffRanks();
        var coeffCnt = coeffRanks.length;

        if (coeffCnt == 0) // Only opaques
        {
            return;
        }

        if (GlCompat.SUPPORTS_DSA)
        {
            coeffsFbo = GL46.glCreateFramebuffers();
            coefficients = new int[coeffCnt];

            int coeffsSum = 0;

            for (int i = 0; i < coeffCnt; i++)
            {
                int rank = coeffRanks[i];
                int buffer = GL46.glCreateTextures(GL46.GL_TEXTURE_2D_ARRAY);
                int depth = MoreMath.ceilingDiv(1 << (rank + 1), 4);

                GL46.glTextureStorage3D(buffer, 1, GL32.GL_RGBA16F, width, height, depth);
                coefficients[i] = buffer;
                coeffsSum += depth;
            }

            renderTransmittanceDrawBuffers = new int[coeffsSum];
            int transmittanceIdx = 0;

            for (int i = 0; i < coeffCnt; i++)
            {
                int rank = coeffRanks[i];
                int depth = MoreMath.ceilingDiv(1 << (rank + 1), 4);

                for (int j = 0; j < depth; j++)
                {
                    GL46.glNamedFramebufferTextureLayer(coeffsFbo, GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx, coefficients[i], 0, j);
                    renderTransmittanceDrawBuffers[transmittanceIdx] = GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx;
                    transmittanceIdx += 1;
                }
            }
        }
        else
        {
            coeffsFbo = GL46.glGenFramebuffers();

            coefficients = new int[coeffCnt];

            int coeffsSum = 0;

            for (int i = 0; i < coeffCnt; i++)
            {
                int rank = coeffRanks[i];
                int buffer = GL32.glGenTextures();
                int depth = MoreMath.ceilingDiv(1 << (rank + 1), 4);

                GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, buffer);
                GL32.glTexImage3D(GL32.GL_TEXTURE_2D_ARRAY, 0, GL32.GL_RGBA16F, width, height, depth, 0, GL46.GL_RGBA, GL46.GL_BYTE, 0);

                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);

                coefficients[i] = buffer;
                coeffsSum += depth;
            }

            GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, coeffsFbo);

            renderTransmittanceDrawBuffers = new int[coeffsSum];
            int transmittanceIdx = 0;

            for (int i = 0; i < coeffCnt; i++)
            {
                int rank = coeffRanks[i];
                int depth = MoreMath.ceilingDiv(1 << (rank + 1), 4);

                for (int j = 0; j < depth; j++)
                {
                    GL46.glFramebufferTextureLayer(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx, coefficients[i], 0, j);
                    renderTransmittanceDrawBuffers[transmittanceIdx] = GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx;
                    transmittanceIdx += 1;
                }
            }
        }
    }
}
