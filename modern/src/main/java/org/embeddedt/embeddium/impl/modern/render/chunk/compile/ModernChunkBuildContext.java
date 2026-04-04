package org.embeddedt.embeddium.impl.modern.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderCache;

import java.util.Collections;

public class ModernChunkBuildContext extends ChunkBuildContext {
    public final BlockRenderCache cache;
    private final ObjectOpenHashSet<TextureAtlasSprite> additionalCapturedSprites;
    private boolean captureAdditionalSprites;

    public ModernChunkBuildContext(ClientLevel world, RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
        this.cache = new BlockRenderCache(Minecraft.getInstance(), world);
        this.additionalCapturedSprites = new ObjectOpenHashSet<>();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.cache.cleanup();
        this.additionalCapturedSprites.clear();
        this.captureAdditionalSprites = false;
    }

    public void setCaptureAdditionalSprites(boolean flag) {
        captureAdditionalSprites = flag;
        if(!flag) {
            additionalCapturedSprites.clear();
        }
    }

    public Iterable<TextureAtlasSprite> getAdditionalCapturedSprites() {
        return additionalCapturedSprites.isEmpty() ? Collections.emptySet() : additionalCapturedSprites;
    }

    public void captureAdditionalSprite(TextureAtlasSprite sprite) {
        if(captureAdditionalSprites) {
            additionalCapturedSprites.add(sprite);
        }
    }
}
