package org.taumc.celeritas;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.jspecify.annotations.Nullable;
import org.taumc.celeritas.render.terrain.CeleritasWorldRenderer;

import java.util.ArrayList;

@Mod(Celeritas.MOD_ID)
public class Celeritas {
    public static final String MOD_ID = "celeritas";
    public static final String MOD_NAME = "Celeritas";

    public static final Identifier RENDERER_INFO = Identifier.fromNamespaceAndPath(Celeritas.MOD_ID, "renderer_info");

    public static String VERSION = "[unknown]";

    public Celeritas(ModContainer modContainer, IEventBus modBus) {
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {

        };

        VERSION = modContainer.getModInfo().getVersion().toString();

        modBus.addListener(this::registerDebugEntry);
    }

    private void registerDebugEntry(RegisterDebugEntriesEvent event) {
        event.register(RENDERER_INFO, (displayer, level, clientChunk, serverChunk) -> {
            ArrayList<String> strings = new ArrayList<>();
            strings.add("%s%s Renderer (%s)".formatted(ChatFormatting.GREEN, MOD_NAME, VERSION));
            var renderer = CeleritasWorldRenderer.instanceNullable();
            if (renderer != null) {
                strings.addAll(renderer.getDebugStrings());
            }
            displayer.addToGroup(RENDERER_INFO, strings);
        });
        event.includeInProfile(RENDERER_INFO, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.IN_OVERLAY);
    }
}
