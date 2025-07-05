package org.embeddedt.embeddium.impl.mixin.features.textures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(Material.class)
public abstract class MaterialMixin implements Function<ResourceLocation, TextureAtlasSprite> {
    @Shadow
    public abstract ResourceLocation atlasLocation();


    @Shadow
    public abstract ResourceLocation texture();

    /**
     * @author embeddedt
     * @reason avoids the allocation in Minecraft.getTextureAtlas
     */
    @Overwrite
    public TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getModelManager().getAtlas(this.atlasLocation()).getSprite(this.texture());
    }
}
