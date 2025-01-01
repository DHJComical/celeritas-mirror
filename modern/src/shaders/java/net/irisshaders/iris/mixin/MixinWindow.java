package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.irisshaders.iris.Iris;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Window.class, priority = 1010)
public class MixinWindow {
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V"))
	private void iris$enableDebugContext(WindowEventHandler arg, ScreenManager arg2, DisplayData arg3, String string, String string2) {
		GLFW.glfwDefaultWindowHints();
		if (Iris.getIrisConfig().areDebugOptionsEnabled()) {
			GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
			Iris.logger.info("OpenGL debug context activated.");
		}
	}
}
