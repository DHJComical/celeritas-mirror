package net.irisshaders.iris.mixin;

import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OculusMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String s) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        // Forge 1.20.1 and older load mixins even if there is a mod loading error, but
        // don't load ATs, which causes a ton of support requests from our mixins failing
        // to apply. The solution is to just not apply them ourselves if there is an error.
        return EarlyLoaderServices.INSTANCE.isLoadingNormally();
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        List<String> list = new ArrayList<>();
        try {
            if(MixinService.getService().getBytecodeProvider().getClassNode("io.github.douira.glsl_transformer.ast.transform.JobParameters") != null) {
                list.add("ParametersMixin");
            }
        } catch (ClassNotFoundException | IOException e) {
        }
        return list;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
