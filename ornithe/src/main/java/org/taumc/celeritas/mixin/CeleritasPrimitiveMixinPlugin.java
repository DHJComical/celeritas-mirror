package org.taumc.celeritas.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.impl.util.MixinClassValidator;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.taumc.celeritas.impl.Celeritas;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class CeleritasPrimitiveMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    private Path getMixinPath() {
        String path = "org/taumc/celeritas/mixin";
        var pathOpt = FabricLoader.getInstance().getModContainer(Celeritas.MODID).orElseThrow().findPath("org/taumc/celeritas/mixin");
        if(pathOpt.isPresent()) {
            return pathOpt.get();
        } else {
            try {
                var resource = CeleritasPrimitiveMixinPlugin.class.getResource("/" + path);
                if (resource == null) {
                    return null;
                }
                Path clPath = Path.of(resource.toURI());
                if(Files.exists(clPath)) {
                    return clPath;
                }
            } catch(URISyntaxException ignored) {
            }
            return null;
        }
    }

    private static String mixinClassify(Path baseFolder, Path path) {
        try {
            String className = baseFolder.relativize(path).toString().replace('/', '.').replace('\\', '.');
            return className.substring(0, className.length() - 6);
        } catch(RuntimeException e) {
            throw new IllegalStateException("Error relativizing " + path + " to " + baseFolder, e);
        }
    }

    private static final boolean ENABLED = false;

    @Override
    public List<String> getMixins() {
        Path rootPath = getMixinPath();
        Set<String> possibleMixinClasses = new HashSet<>();
        try(Stream<Path> mixinStream = Files.find(rootPath, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".class"))) {
            mixinStream
                    .map(Path::toAbsolutePath)
                    .filter(MixinClassValidator::isMixinClass)
                    .map(path -> mixinClassify(rootPath, path))
                    .forEach(possibleMixinClasses::add);
        } catch(IOException e) {
            System.err.println("Error reading path");
            e.printStackTrace();
        }

        return List.copyOf(possibleMixinClasses);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
