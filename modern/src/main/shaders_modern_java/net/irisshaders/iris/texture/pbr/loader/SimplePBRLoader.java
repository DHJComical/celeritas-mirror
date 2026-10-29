package net.irisshaders.iris.texture.pbr.loader;

import net.irisshaders.iris.mixin.texture.SimpleTextureAccessor;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SimplePBRLoader implements PBRTextureLoader<SimpleTexture> {
	@Override
	public void load(SimpleTexture texture, ResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer) {
		ResourceLocation location = ((SimpleTextureAccessor) texture).getLocation();

		AbstractTexture normalTexture = createPBRTexture(location, resourceManager, PBRType.NORMAL);
		AbstractTexture specularTexture = createPBRTexture(location, resourceManager, PBRType.SPECULAR);

		if (normalTexture != null) {
			pbrTextureConsumer.acceptNormalTexture(normalTexture);
		}
		if (specularTexture != null) {
			pbrTextureConsumer.acceptSpecularTexture(specularTexture);
		}
	}

	@Nullable
	protected AbstractTexture createPBRTexture(ResourceLocation imageLocation, ResourceManager resourceManager, PBRType pbrType) {
		ResourceLocation pbrImageLocation = imageLocation.withPath(pbrType::appendSuffix);

		if (PBRType.hasDirectionalSiblings(pbrImageLocation, resourceManager)) {
			// Looks like a cardinal-direction texture set (e.g. "_n"/"_s"/"_e"/"_w" for block faces),
			// not an actual PBR map. Don't treat it as one.
			return null;
		}

		SimpleTexture pbrTexture = new SimpleTexture(pbrImageLocation);
		try {
			pbrTexture.load(resourceManager);
		} catch (IOException e) {
			return null;
		}

		return pbrTexture;
	}
}
