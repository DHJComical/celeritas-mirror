package net.irisshaders.iris.compat.flywheel.indirect;

import net.irisshaders.iris.compat.flywheel.engine.ProgramGroupDepthInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlIndirectPrograms;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import dev.engine_room.flywheel.backend.gl.GlTextureUnit;
import dev.engine_room.flywheel.lib.math.MoreMath;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL46;

public class ClrwlDepthPyramid
{
	private final ClrwlProgramGroup programGroup;
	private final ClrwlIndirectPrograms programs;
	private final IrisRenderingPipeline irisPipeline;

	public int pyramidTextureId = -1;

	private int lastWidth = -1;
	private int lastHeight = -1;

	public ClrwlDepthPyramid(ClrwlProgramGroup programGroup, ClrwlIndirectPrograms programs, IrisRenderingPipeline irisPipeline)
	{
		this.programGroup = programGroup;
		this.programs = programs;
		this.irisPipeline = irisPipeline;
	}

	public void generate()
	{
		var depthInfo = ProgramGroupDepthInfo.of(irisPipeline, programGroup);
		int depthBufferId = depthInfo.textureId();
		int width = mip0Size(depthInfo.width());
		int height = mip0Size(depthInfo.height());

		int mipLevels = getImageMipLevels(width, height);

		createPyramidMips(mipLevels, width, height);

		GL46.glMemoryBarrier(GL46.GL_FRAMEBUFFER_BARRIER_BIT);

		GlTextureUnit.T0.makeActive();
		GlStateManager._bindTexture(depthBufferId);

		var downsampleFirstProgram = programs.getDownsampleFirstProgram();
		downsampleFirstProgram.bind();

		GL46.glBindImageTexture(1, pyramidTextureId, 0, false, 0, GL32.GL_WRITE_ONLY, GL32.GL_R32F);
		GL46.glDispatchCompute(MoreMath.ceilingDiv(width << 1, 64), MoreMath.ceilingDiv(height << 1, 64), 1);

		var downsampleSecondProgram = programs.getDownsampleSecondProgram();
		downsampleSecondProgram.bind();
		downsampleSecondProgram.setUInt("mip_levels", mipLevels);

		for (int baseMipLevel = 0; baseMipLevel + 1 < mipLevels; baseMipLevel += 6)
		{
			GL46.glMemoryBarrier(GL46.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

			downsampleSecondProgram.setUInt("base_mip_level", baseMipLevel);

			for (int i = 0; i < Math.min(7, mipLevels - baseMipLevel); i++)
			{
				GL46.glBindImageTexture(i, pyramidTextureId, baseMipLevel + i, false, 0, GL32.GL_WRITE_ONLY, GL32.GL_R32F);
			}

			GL46.glDispatchCompute(MoreMath.ceilingDiv(width >> baseMipLevel, 64), MoreMath.ceilingDiv(height >> baseMipLevel, 64), 1);
		}

		GL46.glMemoryBarrier(GL46.GL_TEXTURE_FETCH_BARRIER_BIT);
	}

	public void bindForCull()
	{
		GlTextureUnit.T0.makeActive();
		GlStateManager._bindTexture(pyramidTextureId);
	}

	public void delete()
	{
		if (pyramidTextureId != -1)
		{
			GL32.glDeleteTextures(pyramidTextureId);
			pyramidTextureId = -1;
		}
	}

	private void createPyramidMips(int mipLevels, int width, int height)
	{
		if (lastWidth == width && lastHeight == height)
		{
			return;
		}

		lastWidth = width;
		lastHeight = height;

		delete();

		pyramidTextureId = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);
		GL46.glTextureStorage2D(pyramidTextureId, mipLevels, GL32.GL_R32F, width, height);

		GL46.glTextureParameteri(pyramidTextureId, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
		GL46.glTextureParameteri(pyramidTextureId, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
		GL46.glTextureParameteri(pyramidTextureId, GL32.GL_TEXTURE_COMPARE_MODE, GL32.GL_NONE);
		GL46.glTextureParameteri(pyramidTextureId, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
		GL46.glTextureParameteri(pyramidTextureId, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
	}

	public static int mip0Size(int screenSize)
	{
		return Integer.highestOneBit(screenSize);
	}

	public static int getImageMipLevels(int width, int height)
	{
		int result = 1;

		while (width > 1 && height > 1)
		{
			result++;
			width >>= 1;
			height >>= 1;
		}

		return result;
	}
}
