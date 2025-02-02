package net.irisshaders.batchedentityrendering.impl.wrappers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.iris.layer.OuterWrappedRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class OuterWrappingMultiBufferSource implements MultiBufferSource {
    private final Object2ObjectOpenHashMap<RenderType, RenderType> cachedWrappers = new Object2ObjectOpenHashMap<>();
    private MultiBufferSource targetBufferSource;
    private final String name;
    private final RenderStateShard extra;

    public OuterWrappingMultiBufferSource(String name, RenderStateShard extra) {
        this.name = name;
        this.extra = extra;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        var realType = cachedWrappers.get(renderType);
        if (realType == null) {
            realType = OuterWrappedRenderType.wrapExactlyOnce(name, renderType, extra);
            cachedWrappers.put(renderType, realType);
        }
        return targetBufferSource.getBuffer(realType);
    }

    public void setTargetBufferSource(MultiBufferSource targetBufferSource) {
        this.targetBufferSource = targetBufferSource;
    }
}
