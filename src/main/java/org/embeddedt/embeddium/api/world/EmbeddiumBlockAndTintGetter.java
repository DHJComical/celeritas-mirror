package org.embeddedt.embeddium.api.world;

//? if >=1.15 {
import net.minecraft.world.level.BlockAndTintGetter;
public interface EmbeddiumBlockAndTintGetter extends BlockAndTintGetter {
}

//?} else {
/*import net.minecraft.world.level.BlockAndBiomeGetter;

public interface EmbeddiumBlockAndTintGetter extends BlockAndBiomeGetter {
}
*///?}


