package net.irisshaders.iris.vertices;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import org.jetbrains.annotations.Nullable;

/**
 * Some annoying global state needed for rendering.
 */
public class ImmediateState {
    public static final ThreadLocal<Boolean> skipExtension = ThreadLocal.withInitial(() -> false);
	public static boolean isRenderingLevel = false;
	public static boolean usingTessellation = false;
	public static boolean renderWithExtendedVertexFormat = true;
	//? if >=26.1 {
	/*// Whether buffer sources are creating builders for the level. Only those are extended on 26.1, since their meshes
	// are drawn by RenderType.draw, which draws them with their extended format.
	public static boolean extendBufferSourceBuilders = false;
	// The format of the mesh RenderType.draw is drawing, if it differs from its pipeline's.
	@Nullable
	public static VertexFormat meshFormatOverride;
	*///?}

	/**
	 * Whether a buffer builder created now should write Iris's extended vertex format.
	 */
	public static boolean shouldExtendNewBuffers() {
		//? if >=26.1 {
		/*return extendBufferSourceBuilders && WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat();
		*///?} else
		return !skipExtension.get() && WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat();
	}
}
