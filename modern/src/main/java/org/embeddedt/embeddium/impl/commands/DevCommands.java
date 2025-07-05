package org.embeddedt.embeddium.impl.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.ComponentUtil;

import static net.minecraft.commands.Commands.*;

public class DevCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("embeddium")
                .then(literal("toggle_pass").then(argument("pass", TerrainRenderPassArgumentType.type()).executes(context -> {
                    TerrainRenderPass pass = TerrainRenderPassArgumentType.getPass(context, "pass");
                    CeleritasWorldRenderer.instance().getRenderSectionManager().toggleRenderingForTerrainPass(pass);
                    context.getSource().sendSuccess(/*? if >=1.20 {*/() -> /*?}*/ ComponentUtil.literal("Toggled rendering of " + pass.name()), true);
                    return Command.SINGLE_SUCCESS;
                }))));
    }
}
