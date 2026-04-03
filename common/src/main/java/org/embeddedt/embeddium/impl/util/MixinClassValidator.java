package org.embeddedt.embeddium.impl.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MixinClassValidator {
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";

    /**
     * Walks {@code packageRoot} recursively and returns the relative dot-separated class names
     * (without {@code .class} suffix) for every class that carries a {@code @Mixin} annotation.
     */
    public static List<String> scanMixinFolder(Path packageRoot) {
        List<String> mixins = new ArrayList<>();
        try (Stream<Path> stream = Files.find(packageRoot, Integer.MAX_VALUE,
                (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".class"))) {
            stream.map(Path::toAbsolutePath)
                    .filter(MixinClassValidator::isMixinClass)
                    .map(path -> classifyMixin(packageRoot, path))
                    .forEach(mixins::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mixins;
    }

    private static String classifyMixin(Path baseFolder, Path path) {
        try {
            String className = baseFolder.relativize(path).toString().replace('/', '.').replace('\\', '.');
            return className.substring(0, className.length() - 6);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Error relativizing " + path + " to " + baseFolder, e);
        }
    }

    public static boolean isMixinClass(Path classPath) {
        byte[] bytecode;

        try {
            bytecode = Files.readAllBytes(classPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return isMixinClass(fromBytecode(bytecode));
    }

    public static ClassNode fromBytecode(byte[] bytecode) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(bytecode);
        reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return node;
    }

    public static boolean isMixinClass(ClassNode node) {
        if(node.invisibleAnnotations == null) {
            return false;
        }

        return node.invisibleAnnotations.stream().anyMatch(annotation -> annotation.desc.equals(MIXIN_DESC));
    }
}
