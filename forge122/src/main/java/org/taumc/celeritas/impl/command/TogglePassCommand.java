package org.taumc.celeritas.impl.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class TogglePassCommand extends CommandBase {
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getName() {
        return "celeritas_toggle_pass";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/celeritas_toggle_pass [pass_name]";
    }

    private static Stream<TerrainRenderPass> getAllPasses() {
        var renderer = CeleritasWorldRenderer.instanceNullable();

        if (renderer == null) {
            return Stream.empty();
        }

        return renderer.getRenderPassConfiguration().getAllKnownRenderPasses();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return new ArrayList<>(getAllPasses().map(TerrainRenderPass::name).toList());
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if(args.length < 1) {
            sender.sendMessage(new TextComponentString("Pass name must be provided"));
            return;
        }

        Optional<TerrainRenderPass> foundPass = getAllPasses().filter(pass -> pass.name().equals(args[0])).findFirst();
        if (foundPass.isPresent()) {
            CeleritasWorldRenderer.instance().getRenderSectionManager().toggleRenderingForTerrainPass(foundPass.get());
        } else {
            sender.sendMessage(new TextComponentString("Pass " + args[0] + " not found"));
        }
    }
}
