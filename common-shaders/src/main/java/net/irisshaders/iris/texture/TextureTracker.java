package net.irisshaders.iris.texture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.jetbrains.annotations.Nullable;

public class TextureTracker {
	public static final TextureTracker INSTANCE = new TextureTracker();

	private static Runnable bindTextureListener;

	static {
		StateUpdateNotifiers.bindTextureNotifier = listener -> bindTextureListener = listener;
	}

	private final Int2ObjectMap<MCAbstractTexture> textures = new Int2ObjectOpenHashMap<>();

	private boolean lockBindCallback;

	private TextureTracker() {
	}

	public void trackTexture(int id, MCAbstractTexture texture) {
		textures.put(id, texture);
	}

	@Nullable
	public MCAbstractTexture getTexture(int id) {
		return textures.get(id);
	}

	public void onSetShaderTexture(int unit, int id) {
		if (lockBindCallback) {
			return;
		}
		if (unit == 0) {
			lockBindCallback = true;
			if (bindTextureListener != null) {
				bindTextureListener.run();
			}
			WorldRenderingPipeline pipeline = IrisCommon.getPipelineManager().getPipelineNullable();
			if (pipeline != null) {
				pipeline.onSetShaderTexture(id);
			}
			// Reset texture state
			IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 0, id);
			lockBindCallback = false;
		}
	}

	public void onDeleteTexture(int id) {
		textures.remove(id);
	}
}
