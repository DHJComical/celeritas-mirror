package org.embeddedt.embeddium.gradle.mdg.remapper;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.extras.ClassAnalysisDescCompleter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class ReobfuscateCodeAndMixinsTask extends Jar {
    @InputFile
    public abstract RegularFileProperty getTsrgMappings();

    @InputFile
    public abstract RegularFileProperty getDeobfMinecraftJar();

    @Optional
    @InputFiles
    public abstract ConfigurableFileCollection getClasspath();

    @InputFile
    public abstract RegularFileProperty getInput();

    private void remap(File inputJar, File outputJar) throws IOException {
        // Load TSRG mappings
        MemoryMappingTree mappingTree = new MemoryMappingTree();
        Path mappingsPath = getTsrgMappings().getAsFile().get().toPath();
        MappingReader.read(mappingsPath, mappingTree);
        ClassAnalysisDescCompleter.process(getDeobfMinecraftJar().getAsFile().get().toPath(), mappingTree.getSrcNamespace(), mappingTree);

        IMappingProvider mappings = TinyRemapperMappingsHelper.create(mappingTree, "source", "target");
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .extension(new MixinExtension())
                .withMappings(mappings)
                .build();

        // Delete the output jar, otherwise we will just update the changed entries and not remove deleted entries.
        if (outputJar.exists()) {
            if (!outputJar.delete()) {
                getLogger().warn("Failed to delete jar {}", outputJar.getAbsolutePath());
            }
        }

        // Create output consumer
        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(outputJar.toPath()).assumeArchive(true).build()) {
            outputConsumer.addNonClassFiles(inputJar.toPath()); // optional: copy resources

            // Run remap
            remapper.readInputs(inputJar.toPath());
            remapper.readClassPath(getClasspath().getFiles().stream().map(File::toPath).filter(Files::exists).toArray(Path[]::new));
            remapper.apply(outputConsumer);
            remapper.finish();
        }
    }

    @TaskAction
    @Override
    public void copy() {
        try {
            remap(getInput().getAsFile().get(), getArchiveFile().get().getAsFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
