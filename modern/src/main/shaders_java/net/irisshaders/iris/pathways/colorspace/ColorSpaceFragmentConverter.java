package net.irisshaders.iris.pathways.colorspace;

import com.google.common.collect.ImmutableSet;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.program.Program;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pathways.FullScreenQuadRenderer;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.taumc.celeritas.lwjgl.GL30;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class ColorSpaceFragmentConverter implements ColorSpaceConverter {
	private int width;
	private int height;
	private ColorSpace colorSpace;
	private Program program;
	private GlFramebuffer framebuffer;
	private int swapTexture;

	private int target;

	public ColorSpaceFragmentConverter(int width, int height, ColorSpace colorSpace) {
		rebuildProgram(width, height, colorSpace);
	}

	public void rebuildProgram(int width, int height, ColorSpace colorSpace) {
        // Note: The raw GL calls in here _should_ be safe enough not to conflict with the Mojang state manager.
		if (program != null) {
			program.destroy();
			program = null;
			framebuffer.destroy();
			framebuffer = null;
            LWJGL.glDeleteTextures(swapTexture);
			swapTexture = 0;
		}

		this.width = width;
		this.height = height;
		this.colorSpace = colorSpace;

		String vertexSource;
		String source;
		try {
			vertexSource = new String(IOUtils.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/colorSpace.vsh"))), StandardCharsets.UTF_8);
			source = new String(IOUtils.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/colorSpace.csh"))), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		List<StringPair> defineList = new ArrayList<>();
		defineList.add(new StringPair("CURRENT_COLOR_SPACE", String.valueOf(colorSpace.ordinal())));

		for (ColorSpace space : ColorSpace.values()) {
			defineList.add(new StringPair(space.name(), String.valueOf(space.ordinal())));
		}
		source = JcppProcessor.glslPreprocessSource(source, defineList);

		ProgramBuilder builder = ProgramBuilder.begin("colorSpaceFragment", vertexSource, null, source, ImmutableSet.of());

		builder.uniformMatrix(UniformUpdateFrequency.ONCE, "projection", () -> new Matrix4f(2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -1, -1, 0, 1));
		builder.addDynamicSampler(() -> target, "readImage");

		swapTexture = LWJGL.glGenTextures();
		IrisRenderSystem.texImage2D(swapTexture, GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, width, height, 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, null);

		this.framebuffer = new GlFramebuffer();
		framebuffer.addColorAttachment(0, swapTexture);
		this.program = builder.build();
	}

	public void process(int targetImage) {
		if (colorSpace == ColorSpace.SRGB) return;

		this.target = targetImage;
		program.use();
		framebuffer.bind();
		FullScreenQuadRenderer.INSTANCE.render();
		Program.unbind();
		framebuffer.bindAsReadBuffer();
		IrisRenderSystem.copyTexSubImage2D(targetImage, GL30.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
	}
}
