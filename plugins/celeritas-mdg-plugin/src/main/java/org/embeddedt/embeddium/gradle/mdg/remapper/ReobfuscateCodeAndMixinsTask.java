package org.embeddedt.embeddium.gradle.mdg.remapper;

import net.fabricmc.mappingio.extras.ClassAnalysisDescCompleter;
import net.fabricmc.mappingio.format.srg.TsrgFileReader;
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

import java.io.BufferedReader;
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
        try (BufferedReader reader = Files.newBufferedReader(getTsrgMappings().getAsFile().get().toPath())) {
            TsrgFileReader.read(reader, mappingTree);
        }
        ClassAnalysisDescCompleter.process(getDeobfMinecraftJar().getAsFile().get().toPath(), mappingTree.getSrcNamespace(), mappingTree);

        IMappingProvider mappings = TinyRemapperMappingsHelper.create(mappingTree, "source", "target");
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .extension(new MixinExtension())
                .withMappings(mappings)
                .build();

        // Create output consumer
        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(outputJar.toPath()).assumeArchive(true).build()) {
            outputConsumer.addNonClassFiles(inputJar.toPath()); // optional: copy resources

            // Run remap
            remapper.readInputs(inputJar.toPath());
            remapper.readClassPath(getClasspath().getFiles().stream().map(File::toPath).toArray(Path[]::new));
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
