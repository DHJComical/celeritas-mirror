package org.embeddedt.embeddium.impl.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.ComponentUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class TerrainRenderPassArgumentType implements ArgumentType<TerrainRenderPass> {
    private static final List<String> EXAMPLES = Arrays.asList("solid", "translucent");
    public static final SimpleCommandExceptionType UNKNOWN_PASS = new SimpleCommandExceptionType(ComponentUtil.literal("Unknown terrain render pass"));

    public static TerrainRenderPassArgumentType type() {
        return new TerrainRenderPassArgumentType();
    }

    public static TerrainRenderPass getPass(final CommandContext<?> context, final String name) {
        return context.getArgument(name, TerrainRenderPass.class);
    }

    private static Stream<TerrainRenderPass> getAllRenderPasses() {
        return CeleritasWorldRenderer.instance().getRenderPassConfiguration().getAllKnownRenderPasses();
    }

    @Override
    public TerrainRenderPass parse(final StringReader reader) throws CommandSyntaxException {
        String word = reader.readUnquotedString();
        return getAllRenderPasses().filter(pass -> pass.name().equals(word)).findFirst()
                .orElseThrow(UNKNOWN_PASS::create);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(() -> getAllRenderPasses().map(TerrainRenderPass::name).iterator(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
