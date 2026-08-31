package org.embeddedt.embeddium.gradle.mdg.remapper;

import com.google.gson.JsonParser;
import net.neoforged.srgutils.IMappingFile;
import net.neoforged.srgutils.IRenamer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@CacheableTask
public abstract class GenerateNamedToIntermediaryTSRGTask extends DefaultTask {
    @Input
    public abstract Property<String> getForgeVersion();

    @OutputFile
    public abstract RegularFileProperty getTSRGPath();

    private static final String FORGE_MAVEN = "https://maven.minecraftforge.net/";

    /**
     * Translates a Maven coordinate ({@code group:artifact:version[:classifier][@extension]}) into
     * its artifact URL on the Forge Maven.
     */
    private static URL artifactUrl(String coordinate) throws IOException {
        String coord = coordinate;
        String extension = "jar";
        int at = coord.indexOf('@');
        if (at >= 0) {
            extension = coord.substring(at + 1);
            coord = coord.substring(0, at);
        }
        String[] parts = coord.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Not a valid Maven coordinate: " + coordinate);
        }
        String group = parts[0], artifact = parts[1], version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        return new URL(FORGE_MAVEN + group.replace('.', '/') + '/' + artifact + '/' + version
                + '/' + artifact + '-' + version + classifier + '.' + extension);
    }

    private File resolveFromDependency(String dep) throws IOException {
        URL url = artifactUrl(dep);
        String path = url.getPath();
        File target = new File(getTemporaryDir(), path.substring(path.lastIndexOf('/') + 1));
        try (InputStream is = url.openStream()) {
            Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    @TaskAction
    public void generate() {
        var mojmapMappings = new File(getTemporaryDir(), "client_mappings.txt");
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
