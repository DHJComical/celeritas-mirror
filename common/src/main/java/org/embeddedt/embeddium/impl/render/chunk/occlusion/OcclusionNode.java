package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.AbstractSection;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;

public final class OcclusionNode extends AbstractSection {
    private int incomingDirections;
    private int lastVisibleFrame = -1;

    private int adjacentMask;
    public OcclusionNode
            adjacentDown,
            adjacentUp,
            adjacentNorth,
            adjacentSouth,
            adjacentWest,
            adjacentEast;

    /**
     * The occlusion culling data which determines this chunk's connectedness on the visibility graph.
     */
    @Getter
    @Setter
    private long visibilityData = VisibilityEncoding.NULL;

    private final RenderSection section;
    @Getter
    private final int renderRegionId;

    public OcclusionNode(RenderSection section) {
        super(section.getChunkX(), section.getChunkY(), section.getChunkZ());
        this.section = section;
        this.renderRegionId = section.getRegion().getId();
    }

    public OcclusionNode getAdjacent(int direction) {
        return switch (direction) {
            case GraphDirection.DOWN -> this.adjacentDown;
            case GraphDirection.UP -> this.adjacentUp;
            case GraphDirection.NORTH -> this.adjacentNorth;
            case GraphDirection.SOUTH -> this.adjacentSouth;
            case GraphDirection.WEST -> this.adjacentWest;
            case GraphDirection.EAST -> this.adjacentEast;
            default -> null;
        };
    }

    public void setAdjacentNode(int direction, OcclusionNode node) {
        if (node == null) {
            this.adjacentMask &= ~GraphDirectionSet.of(direction);
        } else {
            this.adjacentMask |= GraphDirectionSet.of(direction);
        }

        switch (direction) {
            case GraphDirection.DOWN -> this.adjacentDown = node;
            case GraphDirection.UP -> this.adjacentUp = node;
            case GraphDirection.NORTH -> this.adjacentNorth = node;
            case GraphDirection.SOUTH -> this.adjacentSouth = node;
            case GraphDirection.WEST -> this.adjacentWest = node;
            case GraphDirection.EAST -> this.adjacentEast = node;
            default -> { }
        }
    }

    public int getAdjacentMask() {
        return this.adjacentMask;
    }

    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
    }

    public int getIncomingDirections() {
        return this.incomingDirections;
    }

    public void addIncomingDirections(int directions) {
        this.incomingDirections |= directions;
    }

    public void setIncomingDirections(int directions) {
        this.incomingDirections = directions;
    }

    public RenderSection getRenderSection() {
        return this.section;
    }

    public int getRegionOriginX() {
        return (this.getChunkX() >> RenderRegion.REGION_WIDTH_SH) * RenderRegion.REGION_BLOCK_WIDTH;
    }

    public int getRegionOriginY() {
        return (this.getChunkY() >> RenderRegion.REGION_HEIGHT_SH) * RenderRegion.REGION_BLOCK_HEIGHT;
    }

    public int getRegionOriginZ() {
        return (this.getChunkZ() >> RenderRegion.REGION_LENGTH_SH) * RenderRegion.REGION_BLOCK_LENGTH;
    }
}
