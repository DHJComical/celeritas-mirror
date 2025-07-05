package org.embeddedt.embeddium.impl.mixin.features.textures.mipmaps;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.api.options.storage.MinecraftOptionsStorage;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.impl.util.NativeImageHelper;
import org.embeddedt.embeddium.impl.util.color.ColorSRGB;
import net.minecraft.client.Minecraft;
//? if >=1.20
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.modern.render.chunk.sprite.SpriteTransparencyLevelHolder;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * This Mixin is partially ported from Iris at <a href="https://github.com/IrisShaders/Iris/blob/41095ac23ea0add664afd1b85c414d1f1ed94066/src/main/java/net/coderbot/iris/mixin/bettermipmaps/MixinTextureAtlasSprite.java">MixinTextureAtlasSprite</a>.
 */
//? if >=1.20 {
@Mixin(SpriteContents.class)
//?} else
/*@Mixin(TextureAtlasSprite.class)*/
public class SpriteContentsMixin implements SpriteTransparencyLevelHolder {
    //? if >=1.20 {
    @Mutable
    @Shadow
    @Final
    private NativeImage originalImage;
    //?}

    //? if >=1.17 {
    @Shadow
    @Mutable
    @Final
    private ResourceLocation name;
    //?} else {
    /*@Shadow
    @Mutable
    @Final
    private TextureAtlasSprite.Info info;
    *///?}

    @Unique
    private SpriteTransparencyLevel embeddium$transparencyLevel;

    // While Fabric allows us to @Inject into the constructor here, that's just a specific detail of FabricMC's mixin
    // fork. Upstream Mixin doesn't allow arbitrary @Inject usage in constructor. However, we can use @ModifyVariable
    // just fine, in a way that hopefully doesn't conflict with other mods.
    //
    // By doing this, we can work with upstream Mixin as well, as is used on Forge. While we don't officially
    // support Forge, since this works well on Fabric too, it's fine to ensure that the diff between Fabric and Forge
    // can remain minimal. Being less dependent on specific details of Fabric is good, since it means we can be more
    // cross-platform.
    //? if >=1.20 {
    @Redirect(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;" +
            /*? if <1.20.2 {*/
            "Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;"
            /*?} else {*/
            /*"Lnet/minecraft/server/packs/resources/ResourceMetadata;"
            *//*?}*/
            + /*? if forge {*/"Lnet/minecraftforge/client/textures/ForgeTextureMetadata;"+ /*?}*/ ")V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/SpriteContents;originalImage:Lcom/mojang/blaze3d/platform/NativeImage;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(SpriteContents instance, NativeImage nativeImage, ResourceLocation identifier) {
        // Only fill in transparent colors if mipmaps are on and the texture name does not contain "leaves".
        // We're injecting after the "name" field has been set, so this is safe even though we're in a constructor.
        embeddium$processTransparentImages(nativeImage, MinecraftOptionsStorage.getMipmapLevels() > 0 && this.name.getPath().startsWith("block/") && !this.name.getPath().contains("leaves"));

        this.originalImage = nativeImage;
    }
    //?} else if >=1.17 {
    /*@Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;name:Lnet/minecraft/resources/ResourceLocation;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(TextureAtlasSprite instance, ResourceLocation name, TextureAtlas pAtlas, TextureAtlasSprite.Info pSpriteInfo, int pMipLevel, int pStorageX, int pStorageY, int pX, int pY, NativeImage pImage) {
        // Only fill in transparent colors if mipmaps are on and the texture name does not contain "leaves".
        // We're injecting after the "name" field has been set, so this is safe even though we're in a constructor.
        embeddium$processTransparentImages(pImage, MinecraftOptionsStorage.getMipmapLevels() > 0 && !name.getPath().contains("leaves"));

        this.name = name;
    }
    *///?} else {
    /*@Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;info:Lnet/minecraft/client/renderer/texture/TextureAtlasSprite$Info;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(TextureAtlasSprite instance, TextureAtlasSprite.Info info, TextureAtlas pAtlas, TextureAtlasSprite.Info pSpriteInfo, int pMipLevel, int pStorageX, int pStorageY, int pX, int pY, NativeImage pImage) {
        // Only fill in transparent colors if mipmaps are on and the texture name does not contain "leaves".
        // We're injecting after the "name" field has been set, so this is safe even though we're in a constructor.
        embeddium$processTransparentImages(pImage, MinecraftOptionsStorage.getMipmapLevels() > 0 && !info.name().getPath().contains("leaves"));

        this.info = info;
    }
    *///?}

    /**
     * Fixes a common issue in image editing programs where fully transparent pixels are saved with fully black colors.
     *
     * This causes issues with mipmapped texture filtering, since the black color is used to calculate the final color
     * even though the alpha value is zero. While ideally it would be disregarded, we do not control that. Instead,
     * this code tries to calculate a decent average color to assign to these fully-transparent pixels so that their
     * black color does not leak over into sampling.
     */
    @Unique
    private void embeddium$processTransparentImages(NativeImage nativeImage, boolean shouldRewriteColors) {
        final long ppPixel = NativeImageHelper.getPointerRGBA(nativeImage);
        final int pixelCount = nativeImage.getHeight() * nativeImage.getWidth();

        // Calculate an average color from all pixels that are not completely transparent.
        // This average is weighted based on the (non-zero) alpha value of the pixel.
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;

        float totalWeight = 0.0f;

        SpriteTransparencyLevel level = SpriteTransparencyLevel.OPAQUE;

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = ColorARGB.unpackAlpha(color);

            // Ignore all fully-transparent pixels for the purposes of computing an average color.
            if (alpha > 0) {
                if(alpha < 255) {
                    level = level.chooseNextLevel(SpriteTransparencyLevel.TRANSLUCENT);
                } else {
                    level = level.chooseNextLevel(SpriteTransparencyLevel.OPAQUE);
                }

                if (shouldRewriteColors) {
                    float weight = (float) alpha;

                    // Make sure to convert to linear space so that we don't lose brightness.
                    r += ColorSRGB.srgbToLinear(ColorARGB.unpackRed(color)) * weight;
                    g += ColorSRGB.srgbToLinear(ColorARGB.unpackGreen(color)) * weight;
                    b += ColorSRGB.srgbToLinear(ColorARGB.unpackBlue(color)) * weight;

                    totalWeight += weight;
                }
            } else {
                level = level.chooseNextLevel(SpriteTransparencyLevel.TRANSPARENT);
            }
        }

        this.embeddium$transparencyLevel = level;

        // Bail if none of the pixels are semi-transparent or we aren't supposed to rewrite colors.
        // We can also bail if the transparency level is OPAQUE, since it indicates none of the pixels
        // will need to be overwritten.
        if (!shouldRewriteColors || this.embeddium$transparencyLevel == SpriteTransparencyLevel.OPAQUE || totalWeight == 0.0f) {
            return;
        }

        r /= totalWeight;
        g /= totalWeight;
        b /= totalWeight;

        // Convert that color in linear space back to sRGB.
        // Use an alpha value of zero - this works since we only replace pixels with an alpha value of 0.
        int averageColor = ColorSRGB.linearToSrgb(r, g, b, 0);

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = ColorARGB.unpackAlpha(color);

            // Replace the color values of pixels which are fully transparent, since they have no color data.
            if (alpha == 0) {
                MemoryUtil.memPutInt(pPixel, averageColor);
            }
        }
    }

    @Override
    public SpriteTransparencyLevel embeddium$getTransparencyLevel() {
        return this.embeddium$transparencyLevel;
    }
}