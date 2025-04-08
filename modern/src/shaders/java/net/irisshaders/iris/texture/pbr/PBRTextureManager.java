package net.irisshaders.iris.texture.pbr;

import java.io.IOException;
import java.nio.file.Path;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.targets.backed.NativeImageBackedSingleColorTexture;
import net.irisshaders.iris.texture.TextureTracker;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader.PBRTextureConsumer;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.jetbrains.annotations.NotNull;

public class PBRTextureManager {
	public static final PBRTextureManager INSTANCE = new PBRTextureManager();


	private static Runnable normalTextureChangeListener;
	private static Runnable specularTextureChangeListener;

	static {
		StateUpdateNotifiers.normalTextureChangeNotifier = listener -> normalTextureChangeListener = listener;
		StateUpdateNotifiers.specularTextureChangeNotifier = listener -> specularTextureChangeListener = listener;
	}

	private final Int2ObjectMap<PBRTextureHolder> holders = new Int2ObjectOpenHashMap<>();
	private final PBRTextureConsumerImpl consumer = new PBRTextureConsumerImpl();

	private NativeImageBackedSingleColorTexture defaultNormalTexture;
	private NativeImageBackedSingleColorTexture defaultSpecularTexture;
	// Not PBRTextureHolderImpl to directly reference fields
	private final PBRTextureHolder defaultHolder = new PBRTextureHolder() {
		@Override
		public @NotNull MCAbstractTexture normalTexture() {
			return (MCAbstractTexture)defaultNormalTexture;
		}

		@Override
		public @NotNull MCAbstractTexture specularTexture() {
			return (MCAbstractTexture)defaultSpecularTexture;
		}
	};

	private PBRTextureManager() {
	}

	private static void dumpTexture(Dumpable dumpable, ResourceLocation id, Path path) {
		try {
			dumpable.dumpContents(id, path);
		} catch (IOException e) {
			IRIS_LOGGER.error("Failed to dump texture {}", id, e);
		}
	}

	private static void closeTexture(MCAbstractTexture texture) {
		try {
			texture.close();
		} catch (Exception e) {
			//
		}
		texture.releaseId();
	}

	public static void notifyPBRTexturesChanged() {
		if (normalTextureChangeListener != null) {
			normalTextureChangeListener.run();
		}

		if (specularTextureChangeListener != null) {
			specularTextureChangeListener.run();
		}
	}

	public void init() {
		defaultNormalTexture = new NativeImageBackedSingleColorTexture(PBRType.NORMAL.getDefaultValue());
		defaultSpecularTexture = new NativeImageBackedSingleColorTexture(PBRType.SPECULAR.getDefaultValue());
	}

	public PBRTextureHolder getHolder(int id) {
		PBRTextureHolder holder = holders.get(id);
		if (holder == null) {
			return defaultHolder;
		}
		return holder;
	}

	public PBRTextureHolder getOrLoadHolder(int id) {
		PBRTextureHolder holder = holders.get(id);
		if (holder == null) {
			holder = loadHolder(id);
			holders.put(id, holder);
		}
		return holder;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private PBRTextureHolder loadHolder(int id) {
		AbstractTexture texture = (AbstractTexture) TextureTracker.INSTANCE.getTexture(id);
		if (texture != null) {
			Class<? extends AbstractTexture> clazz = texture.getClass();
			PBRTextureLoader loader = PBRTextureLoaderRegistry.INSTANCE.getLoader(clazz);
			if (loader != null) {
				int previousTextureBinding = GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding;
				consumer.clear();
				try {
					loader.load(texture, Minecraft.getInstance().getResourceManager(), consumer);
					return consumer.toHolder();
				} catch (Exception e) {
					IRIS_LOGGER.debug("Failed to load PBR textures for texture " + id, e);
				} finally {
					GL_STATE_MANAGER.bindTexture(previousTextureBinding);
				}
			}
		}
		return defaultHolder;
	}

	public void onDeleteTexture(int id) {
		PBRTextureHolder holder = holders.remove(id);
		if (holder != null) {
			closeHolder(holder);
		}
	}

	public void dumpTextures(Path path) {
		for (PBRTextureHolder holder : holders.values()) {
			if (holder != defaultHolder) {
				dumpHolder(holder, path);
			}
		}
	}

	private void dumpHolder(PBRTextureHolder holder, Path path) {
        MCAbstractTexture normalTexture = holder.normalTexture();
        MCAbstractTexture specularTexture = holder.specularTexture();
		if (normalTexture != defaultNormalTexture && normalTexture instanceof PBRDumpable dumpable) {
			dumpTexture(dumpable, dumpable.getDefaultDumpLocation(), path);
		}
		if (specularTexture != defaultSpecularTexture && specularTexture instanceof PBRDumpable dumpable) {
			dumpTexture(dumpable, dumpable.getDefaultDumpLocation(), path);
		}
	}

	public void clear() {
		for (PBRTextureHolder holder : holders.values()) {
			if (holder != defaultHolder) {
				closeHolder(holder);
			}
		}
		holders.clear();
	}

	public void close() {
		clear();
		defaultNormalTexture.close();
		defaultSpecularTexture.close();
	}

	private void closeHolder(PBRTextureHolder holder) {
        MCAbstractTexture normalTexture = holder.normalTexture();
        MCAbstractTexture specularTexture = holder.specularTexture();
		if (normalTexture != defaultNormalTexture) {
			closeTexture(normalTexture);
		}
		if (specularTexture != defaultSpecularTexture) {
			closeTexture(specularTexture);
		}
	}

	private record PBRTextureHolderImpl(MCAbstractTexture normalTexture,
                                        MCAbstractTexture specularTexture) implements PBRTextureHolder {
			@Override
			public @NotNull MCAbstractTexture normalTexture() {
				return normalTexture;
			}

			@Override
			public @NotNull MCAbstractTexture specularTexture() {
				return specularTexture;
			}
	}

	private class PBRTextureConsumerImpl implements PBRTextureConsumer {
		private MCAbstractTexture normalTexture;
		private MCAbstractTexture specularTexture;
		private boolean changed;

		@Override
		public void acceptNormalTexture(@NotNull MCAbstractTexture texture) {
			normalTexture = texture;
			changed = true;
		}

		@Override
		public void acceptSpecularTexture(@NotNull MCAbstractTexture texture) {
			specularTexture = texture;
			changed = true;
		}

		public void clear() {
			normalTexture = (MCAbstractTexture)defaultNormalTexture;
			specularTexture = (MCAbstractTexture)defaultSpecularTexture;
			changed = false;
		}

		public PBRTextureHolder toHolder() {
			if (changed) {
				return new PBRTextureHolderImpl(normalTexture, specularTexture);
			} else {
				return defaultHolder;
			}
		}
	}
}
