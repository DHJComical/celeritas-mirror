package net.irisshaders.iris.targets;

import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import org.lwjgl.opengl.GL30C;

import java.util.function.BooleanSupplier;

/**
 * A framebuffer that has a second variant used on odd frames.
 * <p>
 * A render target that ends the frame flipped has its newest content in the alternate texture, while the next frame
 * expects to find it in the main texture. Rather than copying the content back — a full-resolution read and write per
 * such buffer, every frame — the roles of the two textures are swapped on alternating frames. Anything that resolves
 * a texture by role follows that automatically, but a framebuffer bakes its attachments at creation time, so it needs
 * one variant per parity.
 * <p>
 * The odd variant only exists for framebuffers that actually write to a parity-aware target, which is not known until
 * the entire composite chain has been walked — long after the framebuffers writing to it were built. Until then this
 * behaves exactly like an ordinary framebuffer. Both variants are configured by
 * {@link RenderTargets#configure}, so they cannot drift apart.
 * <p>
 * NB: color attachments are the one thing that differs between the variants, and they are only ever set during
 * configuration. Re-attaching a color texture afterwards would apply to the even variant alone.
 */
public class ParityFramebuffer extends GlFramebuffer {
	private final BooleanSupplier oddParity;

	private GlFramebuffer odd;

	ParityFramebuffer(BooleanSupplier oddParity) {
		this.oddParity = oddParity;
	}

	GlFramebuffer createOddVariant() {
		if (odd != null) {
			throw new IllegalStateException("Framebuffer already has an odd parity variant");
		}

		return odd = new GlFramebuffer();
	}

	boolean hasOddVariant() {
		return odd != null;
	}

	private boolean isOdd() {
		return odd != null && oddParity.getAsBoolean();
	}

	@Override
	public void addDepthAttachment(int texture) {
		super.addDepthAttachment(texture);

		if (odd != null) {
			odd.addDepthAttachment(texture);
		}
	}

	@Override
	public void bind() {
		if (isOdd()) {
			odd.bind();
		} else {
			super.bind();
		}
	}

	@Override
	public void bindAsReadBuffer() {
		if (isOdd()) {
			odd.bindAsReadBuffer();
		} else {
			super.bindAsReadBuffer();
		}
	}

	@Override
	public void bindAsDrawBuffer() {
		if (isOdd()) {
			odd.bindAsDrawBuffer();
		} else {
			super.bindAsDrawBuffer();
		}
	}

	@Override
	public int getId() {
		return isOdd() ? odd.getId() : super.getId();
	}

	@Override
	public int getStatus() {
		int status = super.getStatus();

		if (status == GL30C.GL_FRAMEBUFFER_COMPLETE && odd != null) {
			status = odd.getStatus();
		}

		return status;
	}

	@Override
	protected void destroyInternal() {
		super.destroyInternal();

		if (odd != null) {
			odd.delete();
			odd = null;
		}
	}
}
