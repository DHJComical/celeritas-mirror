package org.embeddedt.embeddium.gradle.mdg.remapper;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

public class TinyRemapperMappingsHelper {
    private TinyRemapperMappingsHelper() { }

    private static IMappingProvider.Member memberOf(String className, String memberName, String descriptor) {
        return new IMappingProvider.Member(className, memberName, descriptor);
    }

    public static IMappingProvider create(MappingTree mappings, String from, String to) {
        return (acceptor) -> {
            final int fromId = mappings.getNamespaceId(from);
            final int toId = mappings.getNamespaceId(to);

            for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
                final String className = classDef.getName(fromId);
                String dstName = classDef.getName(toId);

                if (dstName == null) {
                    dstName = className;
                }

                acceptor.acceptClass(className, dstName);

                for (MappingTree.FieldMapping field : classDef.getFields()) {
                    String desc = field.getDesc(fromId);
                    if (desc == null) {
                        continue;
                    }
                    acceptor.acceptField(memberOf(className, field.getName(fromId), desc), field.getName(toId));
                }

                for (MappingTree.MethodMapping method : classDef.getMethods()) {
                    IMappingProvider.Member methodIdentifier = memberOf(className, method.getName(fromId), method.getDesc(fromId));
                    acceptor.acceptMethod(methodIdentifier, method.getName(toId));
                }
            }
        };
    }
}
