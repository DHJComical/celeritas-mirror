package org.embeddedt.embeddium.impl.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class ClientLevelLambdaRemover {
    public static void removeLambda(ClassNode targetClass) {
        for (var method : targetClass.methods) {
            var iter = method.instructions.iterator();
            while (iter.hasNext()) {
                var insn = iter.next();
                if (insn instanceof MethodInsnNode mNode && mNode.name.contains("celeritas$setupForAnimateTickLambdaReplacement")) {
                    // Look for invokedynamic after this
                    while (iter.hasNext()) {
                        insn = iter.next();
                        if (insn instanceof InvokeDynamicInsnNode) {
                            InsnList replace = new InsnList();
                            replace.add(new InsnNode(Opcodes.POP));
                            replace.add(new FieldInsnNode(Opcodes.GETFIELD, targetClass.name, "embeddium$particleSettingsConsumer", "Ljava/util/function/Consumer;"));
                            // Redirect
                            method.instructions.insert(insn, replace);
                            method.instructions.remove(insn);
                            return;
                        }
                    }
                }
            }
        }
    }
}
