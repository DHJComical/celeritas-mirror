package org.embeddedt.embeddium.impl.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import org.embeddedt.embeddium.impl.common.util.TimeUtil;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.metrics.RenderSectionMetricsTracker;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.ComponentUtil;

import java.util.concurrent.TimeUnit;

import static net.minecraft.commands.Commands.*;

public class DevCommands {
    private static void sendMessage(CommandContext<CommandSourceStack> context, String msg) {
        context.getSource().sendSuccess(/*? if >=1.20 {*/() -> /*?}*/ ComponentUtil.literal(msg), true);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("embeddium")
                .then(literal("toggle_pass").then(argument("pass", TerrainRenderPassArgumentType.type()).executes(context -> {
                    TerrainRenderPass pass = TerrainRenderPassArgumentType.getPass(context, "pass");
                    CeleritasWorldRenderer.instance().getRenderSectionManager().toggleRenderingForTerrainPass(pass);
                    sendMessage(context, "Toggled rendering of " + pass.name());
                    return Command.SINGLE_SUCCESS;
                }))).then(literal("metrics").executes(context -> {
                    var sectionManager = CeleritasWorldRenderer.instance().getRenderSectionManager();
                    var metrics = sectionManager.getJobMetricsTracker().getMetrics();
                    sendMessage(context, "Chunk job metrics:");
                    for (var entry : metrics.entrySet()) {
                        sendMessage(context, " - " + entry.getKey().getSimpleName() + " - " + entry.getValue().getStats().toString(t -> TimeUtil.stringifyTime(t, TimeUnit.NANOSECONDS)));
                    }
                    sendMessage(context, "");
                    sendMessage(context, "Slowest-meshing sections:");
                    sectionManager.getSectionMetricsTracker().getSlowestSections().stream().sorted(RenderSectionMetricsTracker.BY_BUILD_TIME.reversed()).forEach(section -> {
                        sendMessage(context, " - " + section.toString() + " @ " + TimeUtil.stringifyTime(section.getLastBuildDurationNanos(), TimeUnit.NANOSECONDS));
                    });
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
