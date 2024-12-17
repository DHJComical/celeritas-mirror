package org.taumc.celeritas.impl.world.cloned;

import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.embeddedt.embeddium.impl.util.position.SectionPos;

public class ChunkRenderContext {
    private final SectionPos sectionCoord;
    private final ClonedChunkSection[] sections;
    private final StructureBoundingBox volume;

    public ChunkRenderContext(SectionPos sectionCoord, ClonedChunkSection[] sections, StructureBoundingBox volume) {
        this.sectionCoord = sectionCoord;
        this.sections = sections;
        this.volume = volume;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.sectionCoord;
    }

    public StructureBoundingBox getVolume() {
        return this.volume;
    }
}