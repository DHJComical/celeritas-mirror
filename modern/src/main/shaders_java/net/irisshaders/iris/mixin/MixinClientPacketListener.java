package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.shaders.CeleritasShaders;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void iris$showUpdateMessage(ClientboundLoginPacket a, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        Iris.getStoredError().ifPresent(e ->
                Iris.showMessage(Minecraft.getInstance().player, Component.translatable(e instanceof ShaderCompileException ? "iris.load.failure.shader" : "iris.load.failure.generic").append(Component.literal("Copy Info").withStyle(arg -> arg.withUnderlined(true).withColor(ChatFormatting.BLUE).withClickEvent(/*? if <26.1 {*/new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, e.getMessage())/*?} else {*//*new ClickEvent.CopyToClipboard(e.getMessage())*//*?}*/)))));

		if (Iris.loadedIncompatiblePack()) {
			Minecraft.getInstance().gui.setTimes(10, 70, 140);
			CeleritasShaders.logger().warn("Incompatible pack for DH!");
			Iris.showMessage(Minecraft.getInstance().player, Component.literal("This pack doesn't have DH support.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
			Iris.showMessage(Minecraft.getInstance().player, Component.literal("Distant Horizons (DH) chunks won't show up. This isn't a bug, get another shader.").withStyle(ChatFormatting.RED));
		}
	}
}
