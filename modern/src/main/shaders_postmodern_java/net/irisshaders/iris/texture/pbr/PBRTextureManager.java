package net.irisshaders.iris.texture.pbr;

import com.mojang.blaze3d.textures.GpuTexture;
import org.taumc.celeritas.shaders.CeleritasShaders;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.targets.backed.NativeImageBackedSingleColorTexture;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader.PBRTextureConsumer;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The normal and specular textures of each texture draws bind, found by the GPU texture they bind.
 */
public class PBRTextureManager {
	public static final PBRTextureManager INSTANCE = new PBRTextureManager();

	private static Runnable normalTextureChangeListener;
	private static Runnable specularTextureChangeListener;

	static {
		StateUpdateNotifiers.normalTextureChangeNotifier = listener -> normalTextureChangeListener = listener;
		StateUpdateNotifiers.specularTextureChangeNotifier = listener -> specularTextureChangeListener = listener;
	}

	// Weak, so that textures that aren't registered with the texture manager, and so never closed through it, are
	// forgotten once they're gone.
	private final Map<GpuTexture, PBRTextureHolder> holders = new WeakHashMap<>();
	private final Set<GpuTexture> pending = Collections.newSetFromMap(new WeakHashMap<>());
	private final PBRTextureConsumerImpl consumer = new PBRTextureConsumerImpl();
	private NativeImageBackedSingleColorTexture defaultNormalTexture;
	private NativeImageBackedSingleColorTexture defaultSpecularTexture;
	private final PBRTextureHolder defaultHolder = new PBRTextureHolder() {
		@Override
		public @NotNull AbstractTexture normalTexture() {
			return defaultNormalTexture;
		}

		@Override
		public @NotNull AbstractTexture specularTexture() {
			return defaultSpecularTexture;
		}
	};

	private PBRTextureManager() {
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

	/**
	 * Returns the PBR textures of a texture. Loading them draws into textures, which can't happen while a draw is being
	 * set up, so textures not seen before have none until {@link #loadPending()} runs.
	 */
	public PBRTextureHolder getOrLoadHolder(GpuTexture texture) {
		PBRTextureHolder holder = holders.get(texture);

		if (holder == null) {
			pending.add(texture);
			return defaultHolder;
		}

		return holder;
	}

	/**
	 * Loads the PBR textures of the textures seen since the last call. Must be called outside any render pass.
	 */
	public void loadPending() {
		if (pending.isEmpty()) {
			return;
		}

		for (GpuTexture texture : pending) {
			if (!texture.isClosed() && !holders.containsKey(texture)) {
				holders.put(texture, loadHolder(findTexture(texture)));
			}
		}

		pending.clear();
		notifyPBRTexturesChanged();
	}

	@Nullable
	private static AbstractTexture findTexture(GpuTexture gpuTexture) {
		for (AbstractTexture texture : Minecraft.getInstance().getTextureManager().byPath.values()) {
			if (texture.texture == gpuTexture) {
				return texture;
			}
		}

		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private PBRTextureHolder loadHolder(@Nullable AbstractTexture texture) {
		if (texture == null) {
			return defaultHolder;
		}

		PBRTextureLoader loader = PBRTextureLoaderRegistry.INSTANCE.getLoader(texture.getClass());

		if (loader == null) {
			return defaultHolder;
		}

		consumer.clear();

		try {
			loader.load(texture, Minecraft.getInstance().getResourceManager(), consumer);
			return consumer.toHolder();
		} catch (Exception e) {
			CeleritasShaders.logger().debug("Failed to load PBR textures for texture " + texture, e);
			return defaultHolder;
		}
	}

	/**
	 * Closes the PBR textures of a texture that's being closed.
	 */
	public void onTextureClosed(AbstractTexture texture) {
		if (texture.texture == null) {
			return;
		}

		PBRTextureHolder holder = holders.remove(texture.texture);

		if (holder != null && holder != defaultHolder) {
			closeHolder(holder);
		}
	}

	public void clear() {
		for (PBRTextureHolder holder : holders.values()) {
			if (holder != defaultHolder) {
				closeHolder(holder);
			}
		}

		holders.clear();
		pending.clear();
	}

	public void close() {
		clear();

		if (defaultNormalTexture != null) {
			defaultNormalTexture.close();
			defaultSpecularTexture.close();
		}
	}

	private void closeHolder(PBRTextureHolder holder) {
		if (holder.normalTexture() != defaultNormalTexture) {
			holder.normalTexture().close();
		}

		if (holder.specularTexture() != defaultSpecularTexture) {
			holder.specularTexture().close();
		}
	}

	private record PBRTextureHolderImpl(AbstractTexture normalTexture, AbstractTexture specularTexture) implements PBRTextureHolder {
		@Override
		public @NotNull AbstractTexture normalTexture() {
			return normalTexture;
		}

		@Override
		public @NotNull AbstractTexture specularTexture() {
			return specularTexture;
		}
	}

	private class PBRTextureConsumerImpl implements PBRTextureConsumer {
		private AbstractTexture normalTexture;
		private AbstractTexture specularTexture;
		private boolean changed;

		@Override
		public void acceptNormalTexture(@NotNull AbstractTexture texture) {
			normalTexture = texture;
			changed = true;
		}

		@Override
		public void acceptSpecularTexture(@NotNull AbstractTexture texture) {
			specularTexture = texture;
			changed = true;
		}

		public void clear() {
			normalTexture = defaultNormalTexture;
			specularTexture = defaultSpecularTexture;
			changed = false;
		}

		public PBRTextureHolder toHolder() {
			return changed ? new PBRTextureHolderImpl(normalTexture, specularTexture) : defaultHolder;
		}
	}
}
