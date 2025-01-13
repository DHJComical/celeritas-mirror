package net.irisshaders.batchedentityrendering.impl;

import net.minecraft.client.renderer.RenderType;

public record BufferSegment(
        //? if <1.21 {
        com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer renderedBuffer,
        //?} else
        /*com.mojang.blaze3d.vertex.MeshData renderedBuffer,*/
        RenderType type) {
}
