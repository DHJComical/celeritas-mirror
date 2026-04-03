package org.embeddedt.embeddium.impl.mixin;

import org.embeddedt.embeddium.impl.SodiumPreLaunch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.asm.AnnotationProcessingEngine;
import org.embeddedt.embeddium.impl.asm.ClientLevelLambdaRemover;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.embeddedt.embeddium.impl.util.PlatformUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

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
                config = MixinConfig.load(PlatformUtil.getConfigDir().resolve("embeddium-mixins.properties").toFile());
            } catch (Exception e) {
                throw new RuntimeException("Could not load configuration file for " + MODNAME, e);
            }

            this.logger.info("Loaded configuration file for " + MODNAME + ": {} options available, {} override(s) found",
                    config.getOptionCount(), config.getOptionOverrideCount());

            SodiumPreLaunch.onPreLaunch();

            //? if forge && <1.17 {
            /*com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
            if (!net.minecraftforge.fml.loading.FMLLoader.isProduction()) {
                org.spongepowered.asm.mixin.MixinEnvironment.setCompatibilityLevel(org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel.JAVA_18);
            }
            *///?}

            try {
                Class<?> clz = Class.forName("org.embeddedt.embeddium.impl.asm.legacy.LegacyAddonPatcher");
                var installMethod = clz.getDeclaredMethod("install");
                installMethod.invoke(null);
            } catch (ClassNotFoundException ignored) {
            } catch (ReflectiveOperationException e) {
                this.logger.error("Error installing legacy patcher", e);
            }
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

    @Override
    public List<String> getMixins() {
        if (!EarlyLoaderServices.INSTANCE.getDistribution().isClient()) {
            return null;
        }

        return EarlyLoaderServices.INSTANCE.findEarlyMixinClasses(basePackage.replace('.', '/') + "/")
                .stream()
                .filter(this::isMixinEnabled)
                .toList();
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

        //? if shaders {
        if (mixinClassName.equals("net.irisshaders.iris.mixin.MixinGameRenderer")) {
            org.embeddedt.embeddium.impl.asm.ShaderOverridePatcher.patchGameRenderer(targetClass);
        }
        //?}
    }
}
