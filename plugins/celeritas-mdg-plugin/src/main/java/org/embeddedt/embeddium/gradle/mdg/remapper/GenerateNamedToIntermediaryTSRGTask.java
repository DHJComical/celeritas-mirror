package org.embeddedt.embeddium.gradle.mdg.remapper;

import com.google.gson.JsonParser;
import net.neoforged.srgutils.IMappingFile;
import net.neoforged.srgutils.IRenamer;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public abstract class GenerateNamedToIntermediaryTSRGTask extends DefaultTask {
    @Input
    public abstract Property<String> getForgeVersion();

    @OutputFile
    public abstract RegularFileProperty getTSRGPath();

    private File resolveFromDependency(String dep) {
        Configuration configuration = getProject().getConfigurations().detachedConfiguration(
                getProject().getDependencies().create(dep)
        );
        configuration.setTransitive(false);
        configuration.setCanBeResolved(true);
        Set<File> files = configuration.resolve();
        if (files.isEmpty()) {
            throw new RuntimeException("No file resolved for: " + dep);
        } else {
            return files.iterator().next();
        }
    }

    @TaskAction
    public void generate() {
        var mojmapMappings = getProject().getLayout().getBuildDirectory().dir("namedToIntermediaryCeleritas").get().file("client_mappings.txt").getAsFile();
        mojmapMappings.getParentFile().mkdirs();
        String forgeVersion = getForgeVersion().get();
        String mcpUrl = null;
        try {
            DownloadOfficialMappingsTask.run(getForgeVersion().get().split("-")[0], mojmapMappings);
            var forgeUrl = new URL("https://maven.minecraftforge.net/net/minecraftforge/forge/" + forgeVersion + "/forge-" + forgeVersion + "-userdev.jar");
            try (ZipInputStream zs = new ZipInputStream(new BufferedInputStream(forgeUrl.openStream()))) {
                ZipEntry ze;
                while ((ze = zs.getNextEntry()) != null) {
                    if (ze.getName().equals("config.json")) {
                        byte[] config = zs.readAllBytes();
                        var jsonTree = JsonParser.parseString(new String(config, StandardCharsets.UTF_8));
                        mcpUrl = jsonTree.getAsJsonObject().get("mcp").getAsString();
                        break;
                    }
                }
            }
            if (mcpUrl == null) {
                throw new RuntimeException("Unable to find MCP url for Forge version: " + forgeVersion);
            }
            File mcpZip = resolveFromDependency(mcpUrl);
            IMappingFile obfToSrg;
            try (ZipFile zf = new ZipFile(mcpZip)) {
                obfToSrg = IMappingFile.load(zf.getInputStream(zf.getEntry("config/joined.tsrg")));
            }
            IMappingFile srgToObf = obfToSrg.reverse();
            IMappingFile officialToObf = IMappingFile.load(mojmapMappings);
            IMappingFile obfToOfficial = officialToObf.reverse();
            officialToObf.chain(obfToSrg).rename(new IRenamer() {
                @Override
                public String rename(IMappingFile.IClass value) {
                    return obfToOfficial.remapClass(srgToObf.remapClass(value.getMapped()));
                }
            }).write(getTSRGPath().getAsFile().get().toPath(), IMappingFile.Format.TSRG, false);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
