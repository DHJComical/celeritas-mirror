package net.irisshaders.batchedentityrendering.mixin;

//? if <1.21 {
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements MemoryTrackingBuffer {
	@Shadow
	private ByteBuffer buffer;

	@Override
	public long getAllocatedSize() {
		return buffer.capacity();
	}

	@Override
	public long getUsedSize() {
		return buffer.position();
	}

	@Override
	public void freeAndDeleteBuffer() {
		if (buffer == null) return;
		MemoryUtil.getAllocator(false).free(MemoryUtil.memAddress(buffer));
		buffer = null;
	}
}
//?} else {

/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ByteBufferBuilder.class)
public abstract class MixinBufferBuilder implements MemoryTrackingBuffer {
    @Shadow
    private int capacity;

    @Shadow
    private int writeOffset;

    @Shadow
    public abstract void close();

    @Override
    public long getAllocatedSize() {
        return this.capacity;
    }

    @Override
    public long getUsedSize() {
        return this.writeOffset;
    }

    @Override
    public void freeAndDeleteBuffer() {
        this.close();
    }
}
*///?}