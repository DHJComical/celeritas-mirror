package net.irisshaders.iris.fantastic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

public class IrisParticleRenderTypes {
	public static final ParticleRenderType OPAQUE_TERRAIN = new ParticleRenderType() {
		public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
            ParticleRenderType.TERRAIN_SHEET.begin(bufferBuilder, textureManager);
			// Cutout is handled by the particle shader for us. We disable blend since these particles should not
            // render with translucency.
			RenderSystem.disableBlend();
		}

		public void end(Tesselator tesselator) {
            ParticleRenderType.TERRAIN_SHEET.end(tesselator);
		}

		public String toString() {
			return "OPAQUE_TERRAIN_SHEET";
		}
	};
}
