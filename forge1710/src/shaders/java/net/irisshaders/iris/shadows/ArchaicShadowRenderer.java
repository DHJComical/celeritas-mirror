package net.irisshaders.iris.shadows;

import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shadows.frustum.ArchaicFrustumHolder;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.compat.mc.MCCamera;
import org.embeddedt.embeddium.compat.mc.MCLevelRenderer;

import java.util.List;

public class ArchaicShadowRenderer extends CommonShadowRenderer {

    public ArchaicShadowRenderer(ProgramSource shadow, PackDirectives directives, ShadowRenderTargets shadowRenderTargets, boolean separateHardwareSamplers) {
        super(shadow, directives, shadowRenderTargets, separateHardwareSamplers);


    }

    @Override
    protected void initFrustumHolders() {
        this.terrainFrustumHolder = new ArchaicFrustumHolder();
        this.entityFrustumHolder = new ArchaicFrustumHolder();
    }

    @Override
    public void destroy() {

    }

    @Override
    protected String getEntitiesDebugString() {
        return (shouldRenderEntities || shouldRenderPlayer) ? (renderedShadowEntities + "/" + Minecraft.getMinecraft().theWorld.loadedEntityList.size()) : "disabled by pack";
    }

    @Override
    protected String getBlockEntitiesDebugString() {
        return (shouldRenderBlockEntities || shouldRenderLightBlockEntities) ? renderedShadowBlockEntities + "" : "disabled by pack"; // TODO: + "/" + MinecraftClient.getInstance().world.blockEntities.size();
    }

    @Override
    protected void addBuffersDebugText(List<String> messages) {

    }

    @Override
    public void renderShadows(MCLevelRenderer levelRendererIn, MCCamera playerCamera) {

    }
}
