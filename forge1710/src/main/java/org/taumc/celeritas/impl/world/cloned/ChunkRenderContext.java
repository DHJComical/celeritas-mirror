package org.taumc.celeritas.impl.world.cloned;

import com.github.bsideup.jabel.Desugar;
import org.embeddedt.embeddium.impl.util.position.SectionPos;

@Desugar
public record ChunkRenderContext(SectionPos origin) {

}