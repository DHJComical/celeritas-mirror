package org.embeddedt.embeddium.gradle.mdg.remapper;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import net.neoforged.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.util.*;

public abstract class GenerateATFromAWTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getAccessWidenerPath();

    @OutputFile
    public abstract RegularFileProperty getAccessTransformerPath();

    @InputFile
    @Optional
    public abstract RegularFileProperty getTsrgMappings();

    @TaskAction
    public void generate() throws IOException {
        var widener = getAccessWidenerPath().get().getAsFile();
        var transformer = getAccessTransformerPath().get().getAsFile();
        try (var os = new AccessTransformerWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(transformer))))) {
            AccessWidenerVisitor visitor = os;
            var mappings = getTsrgMappings();
            if (mappings.isPresent()) {
                visitor = new AccessWidenerRemapper(visitor, new SRGRemapper(IMappingFile.load(mappings.getAsFile().get())), "named", "srg");
            }
            var reader = new AccessWidenerReader(visitor);
            try (var is = new BufferedReader(new InputStreamReader(new FileInputStream(widener)))) {
                reader.read(is);
            }
        }
    }

    private sealed interface Entry permits ClassEntry, ClassMemberEntry {}

    private record ClassEntry(String className) implements Entry {}
    private record ClassMemberEntry(String owner, String name, String descriptor, boolean isMethod) implements Entry {}

    private static class SRGRemapper extends Remapper {
        private final IMappingFile mappings;

        private SRGRemapper(IMappingFile mappings) {
            this.mappings = mappings;
        }

        @Override
        public String map(String internalName) {
            return this.mappings.remapClass(internalName);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            var clz = this.mappings.getClass(owner);
            return clz != null ? clz.remapField(name) : name;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            var clz = this.mappings.getClass(owner);
            return clz != null ? clz.remapMethod(name, descriptor) : name;
        }
    }

    private static class AccessTransformerWriter implements AccessWidenerVisitor, AutoCloseable {
        private final BufferedWriter writer;
        private final Map<Entry, Set<AccessWidenerReader.AccessType>> detectedAccessTypes = new LinkedHashMap<>();

        private AccessTransformerWriter(BufferedWriter writer) {
            this.writer = writer;
        }

        private void writeLine(String str) {
            try {
                writer.write(str);
                writer.write('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void visitHeader(String namespace) {
            writeLine("# generated AT for " + namespace + " names");
        }

        @Override
        public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
            this.detectedAccessTypes.computeIfAbsent(new ClassEntry(name), $ -> new HashSet<>()).add(access);
        }

        @Override
        public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
            this.detectedAccessTypes.computeIfAbsent(new ClassMemberEntry(owner, name, descriptor, false), $ -> new HashSet<>()).add(access);
        }

        @Override
        public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
            this.detectedAccessTypes.computeIfAbsent(new ClassMemberEntry(owner, name, descriptor, true), $ -> new HashSet<>()).add(access);
        }

        private void writeATEntries() throws IOException {
            for (var entry : this.detectedAccessTypes.entrySet()) {
                String accessKey = entry.getValue().contains(AccessWidenerReader.AccessType.ACCESSIBLE) ? "public" : "protected";
                if (entry.getValue().contains(AccessWidenerReader.AccessType.MUTABLE)) {
                    accessKey += "-f";
                }
                writer.write(accessKey);
                writer.write(" ");
                switch (entry.getKey()) {
                    case ClassEntry(String className) -> writer.write(className.replace('/', '.'));
                    case ClassMemberEntry member -> {
                        writer.write(member.owner.replace('/', '.'));
                        writer.write(' ');
                        if (!member.isMethod) {
                            writer.write(member.name);
                        } else {
                            writer.write(member.name);
                            writer.write(member.descriptor);
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + entry.getKey());
                }
                writer.write('\n');
            }
        }

        @Override
        public void close() throws IOException {
            try {
                this.writeATEntries();
            } finally {
                this.writer.close();
            }
        }
    }
}
