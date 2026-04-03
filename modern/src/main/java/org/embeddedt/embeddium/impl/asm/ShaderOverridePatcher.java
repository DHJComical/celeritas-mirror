package org.embeddedt.embeddium.impl.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

public class ShaderOverridePatcher {
    private static final String SHADER_INSTANCE =
            //? if fabric {
            /*net.fabricmc.loader.api.FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_5944").replace('.', '/');
            *///?} else
            "net/minecraft/client/renderer/ShaderInstance";

    /**
     * Patch all methods in GameRenderer that take no arguments and return a ShaderInstance
     * such that their return value is wrapped in ShaderOverridePatcher.
     */
    public static void patchGameRenderer(ClassNode gameRendererNode) {
        Type returnType = Type.getType("L" + SHADER_INSTANCE + ";");
        for (var m : gameRendererNode.methods) {
            if ((m.access & Opcodes.ACC_STATIC) == 0) {
                continue;
            }
            var methodType = Type.getMethodType(m.desc);
            if (methodType.getArgumentTypes().length != 0) {
                continue;
            }
            if (!methodType.getReturnType().equals(returnType)) {
                continue;
            }
            wrapObjectReturns(m.instructions);
        }
    }

    private static void wrapObjectReturns(InsnList instructions) {
        var insn = instructions.getFirst();
        while (insn != null) {
            if (insn.getOpcode() == Opcodes.ARETURN) {
                // Inject before ARETURN
                InsnList wrapper = new InsnList();

                wrapper.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "net/irisshaders/iris/vanilla/ShaderOverrideEngine",
                        "wrapGameRendererReturn",
                        "(L" + SHADER_INSTANCE + ";)L" + SHADER_INSTANCE + ";",
                        false
                ));

                // Insert before ARETURN
                instructions.insertBefore(insn, wrapper);
            }
            insn = insn.getNext();
        }
    }
}
