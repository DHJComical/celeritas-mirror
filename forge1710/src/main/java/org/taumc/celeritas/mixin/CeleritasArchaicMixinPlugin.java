package org.taumc.celeritas.mixin;

import com.gtnewhorizons.retrofuturabootstrap.SharedConfig;
import com.gtnewhorizons.retrofuturabootstrap.api.RetroFuturaBootstrap;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformerHandle;
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
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CeleritasArchaicMixinPlugin implements IMixinConfigPlugin {
    public static final Logger LOGGER = LogManager.getLogger("CeleritasMixins");

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("Loaded Celeritas mixin plugin");
        if (isClassPresent("com.gtnewhorizons.retrofuturabootstrap.SharedConfig")) {
            RfbSupport.excludeFromLwjglRedirection();
        }
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, CeleritasArchaicMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static final class RfbSupport {
        static void excludeFromLwjglRedirection() {
            // We call LWJGL through our own abstraction, so lwjgl3ify must not rewrite our classes.
            RfbClassTransformerHandle handle = SharedConfig.getRfbTransformers().stream()
                    .filter(transformer -> transformer.id().equals("lwjgl3ify:redirect"))
                    .findFirst()
                    .orElse(null);
            if (handle == null) {
                // RFB without lwjgl3ify; nothing is rewriting us.
                return;
            }
            handle.exclusions().add("org.embeddedt.embeddium");
            handle.exclusions().add("org.taumc.celeritas");
        }
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
        List<FileSystem> fileSystemsToClose = new ArrayList<>();
        List<Path> rootPaths = Stream.of("org.taumc.celeritas.mixin")
                .flatMap(str -> {
                    URL url = CeleritasArchaicMixinPlugin.class.getResource("/" + str.replace('.', '/'));
                    if (url == null) {
                        return Stream.empty();
                    }
                    try {
                        var uri = url.toURI();
                        try {
                            return Stream.of(Paths.get(uri));
                        } catch (FileSystemNotFoundException e) {
                            Map<String, String> env = new HashMap<>();
                            env.put("create", "true");
                            fileSystemsToClose.add(FileSystems.newFileSystem(uri, env));
                            return Stream.of(Paths.get(uri));
                        }
                    } catch (Exception e) {
                        LOGGER.error("Exception making URI", e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
        Set<String> possibleMixinClasses = new HashSet<>();
        for(Path rootPath : rootPaths) {
            try(Stream<Path> mixinStream = Files.find(rootPath, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".class"))) {
                mixinStream
                        .map(Path::toAbsolutePath)
                        .filter(MixinClassValidator::isMixinClass)
                        .map(path -> mixinClassify(rootPath, path))
                        .forEach(possibleMixinClasses::add);
            } catch(IOException e) {
                LOGGER.error("Error reading path", e);
            }
        }
        LOGGER.info("Found {} mixin classes", possibleMixinClasses.size());
        for (var fs : fileSystemsToClose) {
            try {
                fs.close();
            } catch(IOException ignored) {
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(possibleMixinClasses));
    }

    @Override
    public void preApply(String s, org.spongepowered.asm.lib.tree.ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, org.spongepowered.asm.lib.tree.ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
