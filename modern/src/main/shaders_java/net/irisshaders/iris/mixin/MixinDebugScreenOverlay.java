package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Lower priority than the primary mixin
@Mixin(value = DebugScreenOverlay.class, priority = 900)
public abstract class MixinDebugScreenOverlay {
	@Unique
	private static final List<BufferPoolMXBean> iris$pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

	@Unique
	private static final BufferPoolMXBean iris$directPool;

	static {
		BufferPoolMXBean found = null;

		for (BufferPoolMXBean pool : iris$pools) {
			if (pool.getName().equals("direct")) {
				found = pool;
				break;
			}
		}

		iris$directPool = Objects.requireNonNull(found);
	}

	// stackoverflow.com/a/3758880
	@Unique
	private static String iris$humanReadableByteCountBin(long bytes) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			return bytes + " B";
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		return String.format("%.3f %ciB", value / 1024.0, ci.current());
	}

    @ModifyExpressionValue(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> redirectRightTextEarly(ArrayList<String> messages) {
		if (IrisCommon.getIrisConfig().areShadersEnabled()) {
			messages.add("Shaderpack: " + Iris.getCurrentPackName() + (Iris.isFallback() ? " (fallback)" : ""));
			IrisCommon.getCurrentPack().ifPresent(pack -> {
				messages.add(pack.getProfileInfo());
			});
			messages.add("Color space: " + IrisVideoSettings.colorSpace.name());
		}

		messages.add(3, "Direct Buffers: +" + iris$humanReadableByteCountBin(iris$directPool.getMemoryUsed()));

        return messages;
	}

	@Inject(method = "getGameInformation", at = @At("RETURN"))
	private void iris$appendShadowDebugText(CallbackInfoReturnable<List<String>> cir) {
		List<String> messages = cir.getReturnValue();

		//if (!Iris.isSodiumInstalled() && Iris.getCurrentPack().isPresent()) {
		//	messages.add(1, ChatFormatting.YELLOW + "[" + Iris.MODNAME + "] Rubidium isn't installed; you will have poor performance.");
		//	messages.add(2, ChatFormatting.YELLOW + "[" + Iris.MODNAME + "] Install Rubidium if you want to run benchmarks or get higher FPS!");
		//}

		Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.addDebugText(messages));
	}
}
