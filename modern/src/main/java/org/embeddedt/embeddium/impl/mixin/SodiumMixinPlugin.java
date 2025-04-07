package org.embeddedt.embeddium.impl.mixin;

//? if forge && <1.17
/*import com.llamalad7.mixinextras.MixinExtrasBootstrap;*/
import org.embeddedt.embeddium.impl.SodiumPreLaunch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.asm.AnnotationProcessingEngine;
import org.embeddedt.embeddium.impl.asm.ClientLevelLambdaRemover;
import org.embeddedt.embeddium.impl.config.ConfigMigrator;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.embeddedt.embeddium.impl.util.MixinClassValidator;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.embeddedt.embeddium.api.EmbeddiumConstants.MODNAME;

@SuppressWarnings("unused")
public class SodiumMixinPlugin implements IMixinConfigPlugin {
    private final Logger logger = LogManager.getLogger(MODNAME);
    private static MixinConfig config;
    private String basePackage;

    private static boolean hasLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        this.basePackage = mixinPackage;

        if (!hasLoaded) {
            hasLoaded = true;
            try {
                config = MixinConfig.load(ConfigMigrator.handleConfigMigration("embeddium-mixins.properties").toFile());
            } catch (Exception e) {
                throw new RuntimeException("Could not load configuration file for " + MODNAME, e);
            }

            this.logger.info("Loaded configuration file for " + MODNAME + ": {} options available, {} override(s) found",
                    config.getOptionCount(), config.getOptionOverrideCount());

            SodiumPreLaunch.onPreLaunch();

            //? if forge && <1.17
            /*MixinExtrasBootstrap.init();*/

            //? if forge && <=1.20.1
            org.embeddedt.embeddium.impl.asm.legacy.LegacyAddonPatcher.install();
        }


    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        return true;
    }

    private boolean isMixinEnabled(String mixin) {
        MixinOption option = config.getEffectiveOptionForMixin(mixin);

        if (option == null) {
            return true;
        }

        if (option.isOverridden()) {
            String source = "[unknown]";

            if (option.isUserDefined()) {
                source = "user configuration";
            } else if (option.isModDefined()) {
                source = "mods [" + String.join(", ", option.getDefiningMods()) + "]";
            }

            if (option.isEnabled()) {
                this.logger.warn("Force-enabling mixin '{}' as rule '{}' (added by {}) enables it", mixin,
                        option.getName(), source);
            } else {
                this.logger.warn("Force-disabling mixin '{}' as rule '{}' (added by {}) disables it and children", mixin,
                        option.getName(), source);
            }
        }

        return option.isEnabled();
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
        if (!EarlyLoaderServices.INSTANCE.getDistribution().isClient()) {
            return null;
        }

        Set<Path> rootPaths = new HashSet<>();

        Path mixinPackagePath = EarlyLoaderServices.INSTANCE.findEarlyMixinFolder(basePackage.replace('.', '/') + "/");
        if(mixinPackagePath != null) {
            rootPaths.add(mixinPackagePath.toAbsolutePath());
        }

        Set<String> possibleMixinClasses = new HashSet<>();
        for(Path rootPath : rootPaths) {
            try(Stream<Path> mixinStream = Files.find(rootPath, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".class"))) {
                mixinStream
                        .map(Path::toAbsolutePath)
                        .filter(MixinClassValidator::isMixinClass)
                        .map(path -> mixinClassify(rootPath, path))
                        .filter(this::isMixinEnabled)
                        .forEach(possibleMixinClasses::add);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>(possibleMixinClasses);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if(targetClassName.startsWith("org.embeddedt.embeddium.") || targetClassName.startsWith("me.jellysquid.mods.sodium.")) {
            AnnotationProcessingEngine.processClass(targetClass);
        }

        if (mixinClassName.contains("features.render.world.ClientLevelMixin")) {
            ClientLevelLambdaRemover.removeLambda(targetClass);
        }
    }
}
