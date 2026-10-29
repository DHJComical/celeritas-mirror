package net.irisshaders.iris.compat.flywheel;

import dev.engine_room.flywheel.backend.gl.GlTextureUnit;

public class ClrwlSamplers
{
	public static final GlTextureUnit DIFFUSE = GlTextureUnit.T0;
	public static final GlTextureUnit OVERLAY = GlTextureUnit.T1;
	public static final GlTextureUnit LIGHT = GlTextureUnit.T2;
	public static final GlTextureUnit CRUMBLING = GlTextureUnit.T3;
	public static final GlTextureUnit INSTANCE_BUFFER = GlTextureUnit.T4;
	public static final GlTextureUnit LIGHT_LUT = GlTextureUnit.T5;
	public static final GlTextureUnit LIGHT_SECTIONS = GlTextureUnit.T6;

	public static final GlTextureUnit DEPTH_RANGE = GlTextureUnit.T7;
	public static final GlTextureUnit NOISE = GlTextureUnit.T8;
	public static final GlTextureUnit FIRST_COEFFICIENT = GlTextureUnit.T9;

	public static GlTextureUnit getAccumulate(int idx)
	{
		return GlTextureUnit.fromGlEnum(GlTextureUnit.T0.glEnum + idx);
	}

	public static GlTextureUnit getCoefficient(int idx)
	{
		return GlTextureUnit.fromGlEnum(FIRST_COEFFICIENT.glEnum + idx);
	}
}
