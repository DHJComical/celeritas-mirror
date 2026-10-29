package net.irisshaders.iris.uniforms;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
//? if <26.1 {
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.impl.gl.compat.FogHelper;
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
*///?}

import java.util.OptionalLong;

/**
 * The client level's time of day, sky, clouds and fog, which 26.1 moved to world clocks, environment attributes and
 * render state.
 */
public final class VanillaEnvironment {
	private VanillaEnvironment() {
	}

	public static long dayTime(ClientLevel level) {
		//? if <26.1 {
		return level.getDayTime();
		//?} else
		//return level.getDefaultClockTime();
	}

	/**
	 * Returns the time of day the level's dimension is fixed at, if it has one.
	 */
	public static OptionalLong fixedTime(ClientLevel level) {
		//? if <26.1 {
		return level.dimensionType().fixedTime();
		//?} else {
		/*// 26.1 dimensions only record whether their time is fixed, not the time itself.
		return level.dimensionType().hasFixedTime() ? OptionalLong.of(0) : OptionalLong.empty();
		*///?}
	}

	/**
	 * Returns how far through its rotation the sky is, from 0 to 1, as vanilla draws it.
	 */
	public static float skyAngle(ClientLevel level, float tickDelta) {
		//? if <26.1 {
		return level.getTimeOfDay(tickDelta);
		//?} else
		//return probe().getValue(EnvironmentAttributes.SUN_ANGLE, tickDelta) / 360.0F;
	}

	public static int moonPhase(ClientLevel level) {
		//? if <26.1 {
		return level.getMoonPhase();
		//?} else
		//return probe().getValue(EnvironmentAttributes.MOON_PHASE, CapturedRenderingState.INSTANCE.getTickDelta()).index();
	}

	public static float cloudHeight(ClientLevel level) {
		//? if <26.1 {
		return level.effects().getCloudHeight();
		//?} else
		//return probe().getValue(EnvironmentAttributes.CLOUD_HEIGHT, CapturedRenderingState.INSTANCE.getTickDelta());
	}

	/**
	 * Returns the color of the sky as seen from the camera entity.
	 */
	public static Vector3d skyColor(ClientLevel level, Entity camera, float tickDelta) {
		//? if <26.1 {
		Vec3 color = level.getSkyColor(camera.position(), tickDelta);
		return new Vector3d(color.x(), color.y(), color.z());
		//?} else {
		/*int color = probe().getValue(EnvironmentAttributes.SKY_COLOR, tickDelta);
		return new Vector3d(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color));
		*///?}
	}

	/**
	 * Returns where the fog shader packs see begins. On 26.1 that is the environmental fog (water, lava, blindness and
	 * the like), not the render distance fog, matching upstream Iris.
	 */
	public static float fogStart() {
		//? if <26.1 {
		return FogHelper.INSTANCE.getFogStart();
		//?} else
		//return fogData().environmentalStart;
	}

	public static float fogEnd() {
		//? if <26.1 {
		return FogHelper.INSTANCE.getFogEnd();
		//?} else
		//return fogData().environmentalEnd;
	}

	//? if >=26.1 {
	/*private static FogData fogData() {
		return Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.fogData;
	}

	private static EnvironmentAttributeProbe probe() {
		return Minecraft.getInstance().gameRenderer.getMainCamera().attributeProbe();
	}
	*///?}
}
