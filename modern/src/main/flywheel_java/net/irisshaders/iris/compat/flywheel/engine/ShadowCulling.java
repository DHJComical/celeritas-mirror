package net.irisshaders.iris.compat.flywheel.engine;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import net.minecraft.client.Minecraft;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector4f;

// Based on Iris' ShadowRenderer
// https://github.com/IrisShaders/Iris/blob/328655a88202e3bfcf69f36d616cbc555ec53875/common/src/main/java/net/irisshaders/iris/shadows/ShadowRenderer.java#L295

public class ShadowCulling
{
    private final ShadowCullState cullState;
    private final float shadowDist;
    private final float voxelDist;
    private final float distMultiplier;
    private final boolean packHasVoxelization;
    private final float sunPathRotation;

    private boolean shouldRenderShadow = true;
    private float reversedCullDist = 0.0f;
    private float cullDist = 0.0f;
    private final Vector3f shadowLightVectorFromOrigin = new Vector3f(0.0f);

    public ShadowCulling(ProgramSet programSet)
    {
        var directives = programSet.getPackDirectives();
        var clrwlDirectives = programSet.getClrwlPrograms().getClrwlDirectives();
        var shadowDirectives = directives.getShadowDirectives();

        cullState = clrwlDirectives.getShadowCullState();
        shadowDist = shadowDirectives.getDistance();
        voxelDist = shadowDirectives.getVoxelDistance();
        packHasVoxelization = packHasVoxelization(programSet);
        sunPathRotation = directives.getSunPathRotation();

        var multiplier = shadowDirectives.getDistanceRenderMul();

        if (cullState == ShadowCullState.REVERSED && multiplier < 0.0f)
        {
            multiplier = 1.0f;
        }

        distMultiplier = multiplier;
    }

    public void refresh()
    {
        var renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;

        shouldRenderShadow = true;

        Vector4f shadowLightPosition = new CelestialUniforms(sunPathRotation).getShadowLightPositionInWorldSpace();
        shadowLightVectorFromOrigin.set(shadowLightPosition.x(), shadowLightPosition.y(), shadowLightPosition.z());

        if ((cullState == ShadowCullState.DEFAULT && packHasVoxelization) || cullState == ShadowCullState.DISTANCE)
        {
            float distance = shadowDist * distMultiplier;

            if (distance == 0.0f)
            {
                shouldRenderShadow = false;
                reversedCullDist = 0.0f;
                cullDist = 0.0f;
            }
            else
            {
                reversedCullDist = 0.0f;
                cullDist = Math.min(distance, renderDistance);
            }
        }
        else if (cullState == ShadowCullState.REVERSED)
        {
            reversedCullDist = voxelDist * distMultiplier;
            cullDist = Math.min(shadowDist * distMultiplier, renderDistance);
        }
        else // ADVANCED
        {
            var distance = shadowDist * distMultiplier;

            if (distance < 0.0f)
            {
                distance = IrisVideoSettings.shadowDistance * 16;
            }

            reversedCullDist = 0.0f;
            cullDist = Math.min(distance, renderDistance);
        }
    }

    public boolean shouldRenderShadow()
    {
        return shouldRenderShadow;
    }

    public float getReversedCullDist()
    {
        return reversedCullDist;
    }

    public float getCullDist()
    {
        return cullDist;
    }

    public Vector3f getShadowLightVectorFromOrigin()
    {
        return shadowLightVectorFromOrigin;
    }

    private static boolean packHasVoxelization(ProgramSet programSet)
    {
        return programSet.get(ProgramId.Shadow)
                .map(src -> src.getSource(ShaderType.GEOM).isPresent())
                .orElse(false);
    }

    public static boolean useFrustumCulling(ProgramSet programSet, ShadowCullState cullState)
    {
        switch (cullState)
        {
            case DEFAULT ->
            {
                return !packHasVoxelization(programSet);
            }

            case DISTANCE ->
            {
                return false;
            }

            default ->
            {
                return true;
            }
        }
    }
}
