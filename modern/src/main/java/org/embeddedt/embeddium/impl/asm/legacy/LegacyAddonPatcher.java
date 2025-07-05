package org.embeddedt.embeddium.impl.asm.legacy;

//? if forge && <=1.20.1 {

import com.google.common.collect.ImmutableMap;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.embeddedt.embeddium.api.EmbeddiumConstants.MODNAME;

public class LegacyAddonPatcher {
    private static final Logger LOGGER = LogManager.getLogger(MODNAME);

    public static void install() {
        try {
            Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
            launchPluginsField.setAccessible(true);
            LaunchPluginHandler launchPluginHandler = (LaunchPluginHandler) launchPluginsField.get(Launcher.INSTANCE);
            Field pluginsField = LaunchPluginHandler.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            Map<String, ILaunchPluginService> plugins = (Map<String, ILaunchPluginService>)pluginsField.get(launchPluginHandler);
            var service = new Service();
            try {
                plugins.put(service.name(), service);
            } catch (Exception e) {
                var newMap = new LinkedHashMap<>(plugins);
                newMap.put(service.name(), service);
                pluginsField.set(launchPluginHandler, newMap);
            }
        } catch(ReflectiveOperationException e) {
            LOGGER.error("Error installing legacy addon patcher", e);
        }
    }

    static class LegacyAddonRemapper extends Remapper {
        boolean anyChange = false;

        private static Map.Entry<Predicate<String>, Function<String, String>> prefixReplacement(String prefix, String replacement) {
            return Map.entry(s -> s.startsWith(prefix), s -> s.replaceFirst(prefix, replacement));
        }

        private static Map.Entry<Predicate<String>, Function<String, String>> exactReplacement(String name, String replacement) {
            return Map.entry(s -> s.equals(name), s -> replacement);
        }

        private static final ImmutableMap<Predicate<String>, Function<String, String>> MAPPINGS = ImmutableMap.<Predicate<String>, Function<String, String>>builder()
                .put(prefixReplacement("me/jellysquid/mods/sodium/client/gui/options/binding/", "org/embeddedt/embeddium/api/options/binding/"))
                .put(prefixReplacement("me/jellysquid/mods/sodium/client/gui/options/control/", "org/embeddedt/embeddium/api/options/control/"))
                .put(exactReplacement("me/jellysquid/mods/sodium/client/gui/options/storage/OptionStorage", "org/embeddedt/embeddium/api/options/structure/OptionStorage"))
                .put(exactReplacement("org/embeddedt/embeddium/client/gui/options/OptionIdentifier", "org/embeddedt/embeddium/api/options/OptionIdentifier"))
                .put(prefixReplacement("me/jellysquid/mods/sodium/client/gui/options/", "org/embeddedt/embeddium/api/options/structure/"))
                .build();

        @Override
        public String map(String descriptor) {
            for (var entry : MAPPINGS.entrySet()) {
                if (entry.getKey().test(descriptor)) {
                    anyChange = true;
                    return entry.getValue().apply(descriptor);
                }
            }
            return descriptor;
        }
    }

    static class Service implements ILaunchPluginService {
        @Override
        public String name() {
            return "celeritas_legacy";
        }

        private static final EnumSet<Phase> GO = EnumSet.of(Phase.AFTER);
        private static final EnumSet<Phase> NOGO = EnumSet.noneOf(Phase.class);

        private static final List<String> PREFIXES = List.of("nolijium/", "zume/");

        @Override
        public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
            String className = classType.getInternalName();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < PREFIXES.size(); i++) {
                if (className.startsWith(PREFIXES.get(i))) {
                    return GO;
                }
            }
            return NOGO;
        }

        private static final Field[] FIELDS_TO_COPY = Arrays.stream(ClassNode.class.getDeclaredFields()).filter(f -> Modifier.isPublic(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())).toArray(Field[]::new);

        @Override
        public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
            ClassNode result = new ClassNode();

            var remapper = new LegacyAddonRemapper();
            classNode.accept(new ClassRemapper(result, remapper));

            if (remapper.anyChange) {
                // Copy everything into the original class node
                try {
                    for (Field f : FIELDS_TO_COPY) {
                        Object v = f.get(result);
                        f.set(classNode, v);
                    }
                } catch (ReflectiveOperationException e) {
                    LOGGER.error("Error copying fields", e);
                }
                return ComputeFlags.SIMPLE_REWRITE;
            } else {
                return ComputeFlags.NO_REWRITE;
            }
        }
    }
}

//?}