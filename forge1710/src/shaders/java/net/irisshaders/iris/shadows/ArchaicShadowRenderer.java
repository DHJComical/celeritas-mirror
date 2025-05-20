package net.irisshaders.iris.shadows;

import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.IrisConstants;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shadows.frustum.FrustumHolder;
import net.minecraft.client.Minecraft;

import java.util.List;

public class ArchaicShadowRenderer extends CommonShadowRenderer {
    private FrustumHolder terrainFrustumHolder;
    private FrustumHolder entityFrustumHolder;


    public ArchaicShadowRenderer(ProgramSource shadow, PackDirectives directives, ShadowRenderTargets shadowRenderTargets, boolean separateHardwareSamplers) {
        super(shadow, directives, shadowRenderTargets, separateHardwareSamplers);

        this.terrainFrustumHolder = new FrustumHolder();
        this.entityFrustumHolder = new FrustumHolder();


    }

    public void addDebugText(List<String> messages) {
        if (IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance) == 0) {
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Maps: off, shadow distance 0");
            return;
        }

        if (IrisCommon.getIrisConfig().areDebugOptionsEnabled()) {
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Maps: " + debugStringOverall);
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Distance Terrain: " + terrainFrustumHolder.getDistanceInfo() + " Entity: " + entityFrustumHolder.getDistanceInfo());
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Culling Terrain: " + terrainFrustumHolder.getCullingInfo() + " Entity: " + entityFrustumHolder.getCullingInfo());
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Terrain: " + debugStringTerrain
                    + (shouldRenderTerrain ? "" : " (no terrain) ") + (shouldRenderTranslucent ? "" : "(no translucent)"));
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Entities: " + getEntitiesDebugString());
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Block Entities: " + getBlockEntitiesDebugString());

            // if (buffers instanceof DrawCallTrackingRenderBuffers drawCallTracker && (shouldRenderEntities || shouldRenderPlayer)) {
            //     messages.add("[" + IrisConstants.MODNAME + "] Shadow Entity Batching: " + BatchingDebugMessageHelper.getDebugMessage(drawCallTracker));
            // }
        } else {
            messages.add("[" + IrisConstants.MODNAME + "] Shadow info: " + debugStringTerrain);
            messages.add("[" + IrisConstants.MODNAME + "] E: " + renderedShadowEntities);
            messages.add("[" + IrisConstants.MODNAME + "] BE: " + renderedShadowBlockEntities);
        }
    }

    @Override
    protected String getEntitiesDebugString() {
        return (shouldRenderEntities || shouldRenderPlayer) ? (renderedShadowEntities + "/" + Minecraft.getMinecraft().theWorld.loadedEntityList.size()) : "disabled by pack";
    }

    @Override
    protected String getBlockEntitiesDebugString() {
        return (shouldRenderBlockEntities || shouldRenderLightBlockEntities) ? renderedShadowBlockEntities + "" : "disabled by pack"; // TODO: + "/" + MinecraftClient.getInstance().world.blockEntities.size();
    }
}
