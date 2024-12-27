package org.embeddedt.embeddium.impl.world.cloned;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.embeddedt.embeddium.api.MeshAppender;

import java.util.List;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@AllArgsConstructor
@Getter
public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox volume;
    private final List<MeshAppender> meshAppenders;
}
