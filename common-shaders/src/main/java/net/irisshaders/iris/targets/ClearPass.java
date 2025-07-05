package net.irisshaders.iris.targets;

import java.util.Objects;
import java.util.function.IntSupplier;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import org.joml.Vector4f;

public class ClearPass {
	private final Vector4f color;
	private final IntSupplier viewportX;
	private final IntSupplier viewportY;
	private final GlFramebuffer framebuffer;
	private final int clearFlags;

	public ClearPass(Vector4f color, IntSupplier viewportX, IntSupplier viewportY, GlFramebuffer framebuffer, int clearFlags) {
		this.color = color;
		this.viewportX = viewportX;
		this.viewportY = viewportY;
		this.framebuffer = framebuffer;
		this.clearFlags = clearFlags;
	}

	public void execute(Vector4f defaultClearColor) {
		RENDER_SYSTEM.glViewport(0, 0, viewportX.getAsInt(), viewportY.getAsInt());
		framebuffer.bind();

		Vector4f color = Objects.requireNonNull(defaultClearColor);

		if (this.color != null) {
			color = this.color;
		}

		RENDER_SYSTEM.glClearColor(color.x, color.y, color.z, color.w);
		RENDER_SYSTEM.clear(clearFlags, MINECRAFT_SHIM.isOnOSX());
	}

	public GlFramebuffer getFramebuffer() {
		return framebuffer;
	}
}
