package org.taumc.celeritas.mixin;

import com.gtnewhorizons.retrofuturabootstrap.SharedConfig;
import com.gtnewhorizons.retrofuturabootstrap.api.RetroFuturaBootstrap;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformerHandle;
import me.eigenraven.lwjgl3ify.rfb.transformers.LwjglRedirectTransformer;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.util.MixinClassValidator;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class CeleritasVintageMixinPlugin implements IMixinConfigPlugin {
    public static final Logger LOGGER = LogManager.getLogger("CeleritasMixins");

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("Loaded Celeritas mixin plugin");
        // Hack for now
        var handle = SharedConfig.getRfbTransformers().stream().filter(transformer -> transformer.id().equals("lwjgl3ify:redirect")).findFirst().orElseThrow();
        handle.exclusions().add("org.embeddedt.embeddium");
        handle.exclusions().add("org.taumc.celeritas");
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    private static String mixinClassify(Path baseFolder, Path path) {
        try {
            String className = baseFolder.relativize(path).toString().replace('/', '.').replace('\\', '.');
            return className.substring(0, className.length() - 6);
        } catch(RuntimeException e) {
            throw new IllegalStateException("Error relativizing " + path + " to " + baseFolder, e);
        }
    }

    @Override
    public List<String> getMixins() {
        List<Path> rootPaths = Stream.of("org.taumc.celeritas.mixin")
                .flatMap(str -> {
                    URL url = CeleritasVintageMixinPlugin.class.getResource("/" + str.replace('.', '/'));
                    if (url == null) {
                        return Stream.empty();
                    }
                    try {
                        return Stream.of(Path.of(url.toURI()));
                    } catch (Exception e) {
                        LOGGER.error("Exception making URI", e);
                        return Stream.empty();
                    }
                })
                .toList();
        Set<String> possibleMixinClasses = new HashSet<>();
        for(Path rootPath : rootPaths) {
            try(Stream<Path> mixinStream = Files.find(rootPath, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".class"))) {
                mixinStream
                        .map(Path::toAbsolutePath)
                        .filter(MixinClassValidator::isMixinClass)
                        .map(path -> mixinClassify(rootPath, path))
                        .forEach(possibleMixinClasses::add);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("Found {} mixin classes", possibleMixinClasses.size());
        return List.copyOf(possibleMixinClasses);
    }

    @Override
    public void preApply(String s, org.spongepowered.asm.lib.tree.ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, org.spongepowered.asm.lib.tree.ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
