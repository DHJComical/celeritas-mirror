package net.irisshaders.iris.pathways.scaling;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.program.Program;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.gl.texture.PixelFormat;
import net.irisshaders.iris.gl.texture.PixelType;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.pathways.FullScreenQuadRenderer;
import org.apache.commons.io.IOUtils;
import org.joml.Vector2i;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * AMD FidelityFX Super Resolution 1.0, as a pair of full-screen fragment passes.
 *
 * <p>EASU resolves the reduced-resolution world image up to output size into an intermediate
 * texture; RCAS sharpens that intermediate into the destination. Both passes run at output
 * resolution.</p>
 *
 * <p>The filter math lives in {@code upscale/ffx_a.h} and {@code upscale/ffx_fsr1.h}, verbatim
 * copies of AMD's MIT-licensed reference headers (see {@code licenses/fidelityfx-fsr.txt}).
 * They're written to be included directly into GLSL, so rather than translating them we
 * prepend them to our own thin wrappers.</p>
 *
 * <p>FSR 1 expects an input that is already anti-aliased, display-referred and in [0, 1] -
 * exactly what sits in the world render target once the shader pack's {@code final} pass has
 * run, which is where {@link RenderScale} invokes this.</p>
 */
public class FsrUpscaler {
	/**
	 * 4.20 is the floor: {@code ffx_a.h} uses {@code packHalf2x16}, which is only core from
	 * GLSL 4.20 onwards. Everything else it touches is available by 4.00.
	 */
	private static final String GLSL_VERSION = "#version 420 core\n";
	private static final String GPU_DEFINES = "#define A_GPU 1\n#define A_GLSL 1\n";

	private final Program easuProgram;
	private final Program rcasProgram;
	private final GlFramebuffer easuFramebuffer;
	private final int easuTexture;

	private final Vector4i con0 = new Vector4i();
	private final Vector4i con1 = new Vector4i();
	private final Vector4i con2 = new Vector4i();
	private final Vector4i con3 = new Vector4i();
	private final Vector2i maxCoord = new Vector2i();

	private int width;
	private int height;
	private int inputTexture;
	private float sharpnessStops;
	private boolean destroyed;

	private FsrUpscaler(int width, int height) {
		this.width = width;
		this.height = height;

		this.easuTexture = GlStateManager._genTexture();
		allocateIntermediate();

		this.easuFramebuffer = new GlFramebuffer();
		this.easuFramebuffer.addColorAttachment(0, easuTexture);
		this.easuFramebuffer.drawBuffers(new int[]{0});
		this.easuFramebuffer.readBuffer(0);

		int status = this.easuFramebuffer.getStatus();
		if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("FSR intermediate framebuffer is incomplete: " + status);
		}

		this.easuProgram = buildEasu();
		this.rcasProgram = buildRcas();
	}

	/**
	 * @return a ready-to-use upscaler, or null if the programs could not be built on this
	 * driver. Callers are expected to fall back to a plain bilinear blit in that case.
	 */
	public static FsrUpscaler createOrNull(int width, int height) {
		try {
			return new FsrUpscaler(width, height);
		} catch (Exception e) {
			CeleritasShaders.logger().error(
				"Failed to set up FSR upscaling, falling back to bilinear. Note that reported line "
					+ "numbers include the prepended ffx_a.h and ffx_fsr1.h.", e);
			return null;
		}
	}

	private static String readResource(String path) {
		try (InputStream stream = FsrUpscaler.class.getResourceAsStream(path)) {
			return new String(IOUtils.toByteArray(Objects.requireNonNull(stream, path)), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Could not read " + path, e);
		}
	}

	private static String buildFragmentSource(String stageDefine, String wrapper) {
		return GLSL_VERSION + GPU_DEFINES + "#define " + stageDefine + " 1\n"
			+ readResource("/upscale/ffx_a.h")
			+ readResource("/upscale/ffx_fsr1.h")
			+ readResource(wrapper);
	}

	private Program buildEasu() {
		ProgramBuilder builder = ProgramBuilder.begin("fsrEasu",
			GLSL_VERSION + readResource("/upscale/upscale.vsh"), null,
			buildFragmentSource("FSR_EASU_F", "/upscale/fsr_easu.fsh"), ImmutableSet.of());

		builder.addDynamicSampler(() -> inputTexture, "inputImage");
		builder.uniform4ui(UniformUpdateFrequency.PER_FRAME, "easuCon0", () -> con0);
		builder.uniform4ui(UniformUpdateFrequency.PER_FRAME, "easuCon1", () -> con1);
		builder.uniform4ui(UniformUpdateFrequency.PER_FRAME, "easuCon2", () -> con2);
		builder.uniform4ui(UniformUpdateFrequency.PER_FRAME, "easuCon3", () -> con3);

		return builder.build();
	}

	private Program buildRcas() {
		ProgramBuilder builder = ProgramBuilder.begin("fsrRcas",
			GLSL_VERSION + readResource("/upscale/upscale.vsh"), null,
			buildFragmentSource("FSR_RCAS_F", "/upscale/fsr_rcas.fsh"), ImmutableSet.of());

		builder.addDynamicSampler(() -> inputTexture, "inputImage");
		builder.uniform1f(UniformUpdateFrequency.PER_FRAME, "rcasSharpness", () -> sharpnessStops);
		builder.uniform2i(UniformUpdateFrequency.PER_FRAME, "rcasMaxCoord", () -> maxCoord);

		return builder.build();
	}

	private void allocateIntermediate() {
		// RGBA8 matches the main render target, so EASU neither gains nor loses precision here.
		IrisRenderSystem.texImage2D(easuTexture, GL11C.GL_TEXTURE_2D, 0,
			InternalTextureFormat.RGBA8.getGlFormat(), width, height, 0,
			PixelFormat.RGBA.getGlFormat(), PixelType.UNSIGNED_BYTE.getGlFormat(), null);

		// RCAS reads this with texelFetch only, so point sampling is all that is needed.
		IrisRenderSystem.texParameteri(easuTexture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(easuTexture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(easuTexture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL13C.GL_CLAMP_TO_EDGE);
		IrisRenderSystem.texParameteri(easuTexture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL13C.GL_CLAMP_TO_EDGE);
	}

	private void resizeIfNeeded(int newWidth, int newHeight) {
		if (newWidth == width && newHeight == height) {
			return;
		}

		this.width = newWidth;
		this.height = newHeight;
		allocateIntermediate();
	}

	/**
	 * Port of {@code FsrEasuCon} from ffx_fsr1.h. {@code FsrEasuF} takes its constants as
	 * {@code AU4} (raw float bits stored in a {@code uvec4}), so they are bit-cast with
	 * {@link Float#floatToRawIntBits} here rather than uploaded as floats and reinterpreted
	 * in the shader.
	 */
	private void updateEasuConstants(int inputWidth, int inputHeight) {
		float inW = inputWidth;
		float inH = inputHeight;
		float outW = width;
		float outH = height;

		// Output integer position to a pixel position in the input viewport.
		setBits(con0, inW / outW, inH / outH, 0.5f * inW / outW - 0.5f, 0.5f * inH / outH - 0.5f);
		// Viewport pixel position to normalized image space, plus the first gather offset.
		setBits(con1, 1.0f / inW, 1.0f / inH, 1.0f / inW, -1.0f / inH);
		// The remaining gather centers, relative to the first.
		setBits(con2, -1.0f / inW, 2.0f / inH, 1.0f / inW, 2.0f / inH);
		setBits(con3, 0.0f, 4.0f / inH, 0.0f, 0.0f);
	}

	private static void setBits(Vector4i vec, float x, float y, float z, float w) {
		vec.set(Float.floatToRawIntBits(x), Float.floatToRawIntBits(y), Float.floatToRawIntBits(z), Float.floatToRawIntBits(w));
	}

	/**
	 * Resolves {@code source} into {@code destination}, upscaling to the destination's size.
	 *
	 * @param sharpnessStops RCAS sharpening attenuation in stops; 0 is maximum sharpening and
	 *                       each whole step halves it.
	 */
	public void process(RenderTarget source, RenderTarget destination, float sharpnessStops) {
		if (destroyed) {
			throw new IllegalStateException("Tried to use a destroyed FsrUpscaler");
		}

		resizeIfNeeded(destination.width, destination.height);
		updateEasuConstants(source.width, source.height);
		this.maxCoord.set(width - 1, height - 1);
		this.sharpnessStops = sharpnessStops;

		RenderSystem.disableBlend();
		RenderSystem.depthMask(false);
		RenderSystem.colorMask(true, true, true, true);

		FullScreenQuadRenderer.INSTANCE.begin();

		// EASU: the reduced-resolution world image up to output resolution.
		this.inputTexture = source.getColorTextureId();
		RenderSystem.viewport(0, 0, width, height);
		easuFramebuffer.bind();
		easuProgram.use();
		FullScreenQuadRenderer.INSTANCE.renderQuad();

		// RCAS: sharpen the result into the destination. bindWrite(true) restores the
		// destination's own viewport for us.
		this.inputTexture = easuTexture;
		destination.bindWrite(true);
		rcasProgram.use();
		FullScreenQuadRenderer.INSTANCE.renderQuad();

		FullScreenQuadRenderer.INSTANCE.end();

		Program.unbind();
		RenderSystem.depthMask(true);
		RenderSystem.activeTexture(GL13C.GL_TEXTURE0);
	}

	public void destroy() {
		if (destroyed) {
			return;
		}

		destroyed = true;
		easuProgram.destroy();
		rcasProgram.destroy();
		easuFramebuffer.destroy();
		GlStateManager._deleteTexture(easuTexture);
	}
}
