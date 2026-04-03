package org.embeddedt.embeddium.impl.mixin;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SodiumMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("CeleritasMixinPlugin");

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    private static final Pattern CLASS_SUFFIX = Pattern.compile("\\.class$");

    @Override
    public List<String> getMixins() {
        var file = FMLLoader.getCurrent().getLoadingModList().getModFileById("embeddium");
        List<String> mixins = new ArrayList<>();
        String startingFolder = "org/embeddedt/embeddium/impl/mixin";
        file.getFile().getContents().visitContent(startingFolder, (relativePath, resource) -> {
            relativePath = relativePath.substring(startingFolder.length() + 1);
            if (relativePath.indexOf('/') == -1 || relativePath.indexOf('$') != -1) {
                return;
            }
            String mixinName = CLASS_SUFFIX.matcher(relativePath).replaceFirst("").replace('/', '.');
            mixins.add(mixinName);
        });
        LOGGER.info("Loading {} mixins", mixins.size());
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
