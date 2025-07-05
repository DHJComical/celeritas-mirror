package net.irisshaders.iris.texture.pbr;

import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.jetbrains.annotations.NotNull;

public interface PBRTextureHolder {
	@NotNull
	MCAbstractTexture normalTexture();

	@NotNull
    MCAbstractTexture specularTexture();
}
