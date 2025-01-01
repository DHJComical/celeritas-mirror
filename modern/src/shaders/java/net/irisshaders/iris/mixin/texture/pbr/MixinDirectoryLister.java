package net.irisshaders.iris.mixin.texture.pbr;

import net.irisshaders.iris.texture.pbr.PBRType;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DirectoryLister.class)
public class MixinDirectoryLister {
	@Shadow
	@Final
	private String sourcePath;

	@Shadow
	@Final
	private String idPrefix;

	/**
	 * @author Asek3
	 * @reason ModifyArgs on forge
	 */
	@Overwrite
	public void run(ResourceManager resourceManager, SpriteSource.Output output) {
		FileToIdConverter fileToIdConverter = new FileToIdConverter("textures/" + sourcePath, ".png");
		fileToIdConverter.listMatchingResources(resourceManager).forEach((location, resource) -> {
			String basePath = PBRType.removeSuffix(location.getPath());
			if (basePath != null) {
				ResourceLocation baseLocation = location.withPath(basePath);
				if (resourceManager.getResource(baseLocation).isPresent()) {
					return;
				}
			}
			ResourceLocation resourceLocation = fileToIdConverter.fileToId(location).withPrefix(idPrefix);
			output.add(resourceLocation, resource);
		});
	}
}
