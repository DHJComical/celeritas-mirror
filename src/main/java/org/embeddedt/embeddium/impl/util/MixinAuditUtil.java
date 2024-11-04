package org.embeddedt.embeddium.impl.util;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

import java.lang.reflect.Field;
import java.util.List;

public class MixinAuditUtil {
    private static Object getInternal(Class<?> clz, Object o, String fieldName) {
        try {
            Field field = clz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    public static void auditAndExit() {
        var env = MixinEnvironment.getCurrentEnvironment();
        int exitCode = 0;
        try {
            if(env.getActiveTransformer() instanceof IMixinTransformer transformer) {
                var processor = getInternal(transformer.getClass(), transformer, "processor");
                List<IMixinConfig> configs = (List<IMixinConfig>)getInternal(processor.getClass(), processor, "configs");
                var provider = MixinService.getService().getClassProvider();
                for(var config : configs) {
                    for(String target : config.getTargets()) {
                        try {
                            provider.findClass(target, false);
                        } catch(Exception e) {
                            System.err.println("Failed to load " + target);
                            e.printStackTrace();
                            exitCode = 1;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            exitCode = 1;
        }
        try {
            System.exit(exitCode);
        } catch(Throwable e) {
            // Thanks FML
            Runtime.getRuntime().halt(exitCode);
        }
    }
}
