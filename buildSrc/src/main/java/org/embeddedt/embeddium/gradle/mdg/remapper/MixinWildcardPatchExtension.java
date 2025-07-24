package org.embeddedt.embeddium.gradle.mdg.remapper;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.common.data.Annotation;
import net.fabricmc.tinyremapper.extension.mixin.common.data.AnnotationElement;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Ugly hack to fix TinyRemapper blindly deleting the descriptor off a wildcard mixin target.
 */
public class MixinWildcardPatchExtension implements TinyRemapper.Extension {
    private final MappingTree mappingTree;

    public MixinWildcardPatchExtension(MappingTree mappingTree) {
        this.mappingTree = mappingTree;
    }

    @Override
    public void attach(TinyRemapper.Builder builder) {
        builder.extraPostApplyVisitor((t, n) -> new PatchVisitor(n));
    }

    private class PatchVisitor extends ClassVisitor {
        protected PatchVisitor(ClassVisitor next) {
            super(Opcodes.ASM9, next);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor next = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new PatchMethodVisitor(next, name);
        }
    }

    private class PatchMethodVisitor extends MethodVisitor {
        private final String name;
        protected PatchMethodVisitor(MethodVisitor next, String name) {
            super(Opcodes.ASM9, next);
            this.name = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor next = super.visitAnnotation(descriptor, visible);
            if (descriptor.equals(Annotation.INJECT)) {
                return new MixinAnnotationPatchVisitor(next, name);
            } else {
                return next;
            }
        }
    }

    private class MixinAnnotationPatchVisitor extends AnnotationVisitor {
        private final String methodName;
        protected MixinAnnotationPatchVisitor(AnnotationVisitor next, String methodName) {
            super(Opcodes.ASM9, next);
            this.methodName = methodName;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            AnnotationVisitor next = super.visitArray(name);
            if (name.equals(AnnotationElement.METHOD)) {
                return new AnnotationVisitor(Opcodes.ASM9, next) {
                    @Override
                    public void visit(String name, Object value) {
                        if (methodName.equals("iris$overrideShader")) {
                            value = "*" + mappingTree.mapDesc("()Lnet/minecraft/client/renderer/ShaderInstance;", 0);
                        }
                        super.visit(name, value);
                    }
                };
            } else {
                return next;
            }
        }
    }
}
