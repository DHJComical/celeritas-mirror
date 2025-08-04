package org.taumc.celeritas.impl.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.impl.config.ConfigMigrator;
import org.taumc.celeritas.impl.gui.options.TextProvider;
import org.taumc.celeritas.impl.util.ComponentUtil;

public class SodiumGameOptions {
    private static final String DEFAULT_FILE_NAME = "celeritas-options.json";
    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();
    public final PerformanceSettings performance = new PerformanceSettings();
    public final NotificationSettings notifications = new NotificationSettings();
    private boolean readOnly;
    private Path configPath;
    private static final Gson GSON;

    public static SodiumGameOptions defaults() {
        SodiumGameOptions options = new SodiumGameOptions();
        options.configPath = getConfigPath("celeritas-options.json");
        return options;
    }

    public static SodiumGameOptions load() {
        return load("celeritas-options.json");
    }

    public static SodiumGameOptions load(String name) {
        Path path = getConfigPath(name);
        boolean resaveConfig = true;
        SodiumGameOptions config;
        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, SodiumGameOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            } catch (JsonSyntaxException e) {
                CeleritasVintage.logger().error("Could not parse config, will fallback to default settings", e);
                config = new SodiumGameOptions();
                resaveConfig = false;
            }
        } else {
            config = new SodiumGameOptions();
        }

        config.configPath = path;
        config.notifications.forceDisableDonationPrompts = false;

        try {
            if (resaveConfig) {
                config.writeChanges();
            }

            return config;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't update config file", e);
        }
    }

    private static Path getConfigPath(String name) {
        return ConfigMigrator.handleConfigMigration(name);
    }

    /** @deprecated */
    @Deprecated
    public void writeChanges() throws IOException {
        writeToDisk(this);
    }

    public static void writeToDisk(SodiumGameOptions config) throws IOException {
        if (config.isReadOnly()) {
            throw new IllegalStateException("Config file is read-only");
        } else {
            Path dir = config.configPath.getParent();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            } else if (!Files.isDirectory(dir)) {
                throw new IOException("Not a directory: " + dir);
            }

            Path tempPath = config.configPath.resolveSibling(config.configPath.getFileName() + ".tmp");
            Files.writeString(tempPath, GSON.toJson(config));
            Files.move(tempPath, config.configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly() {
        this.readOnly = true;
    }

    public String getFileName() {
        return this.configPath.getFileName().toString();
    }

    static {
        GSON = (new GsonBuilder()).setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().excludeFieldsWithModifiers(new int[]{2}).create();
    }

    public static class PerformanceSettings {
        public int chunkBuilderThreads = 0;
        @SerializedName("always_defer_chunk_updates_v2")
        public boolean alwaysDeferChunkUpdates = true;
        public boolean animateOnlyVisibleTextures = true;
        public boolean useEntityCulling = true;
        public boolean useFogOcclusion = true;
        public boolean useBlockFaceCulling = true;
        public boolean useCompactVertexFormat = false;
        @SerializedName("use_translucent_face_sorting_v2")
        public boolean useTranslucentFaceSorting = true;
        public boolean useRenderPassOptimization = true;
        public boolean useRenderPassConsolidation = true;
        public AsyncOcclusionMode asyncOcclusionMode;

        public PerformanceSettings() {
            this.asyncOcclusionMode = AsyncOcclusionMode.ONLY_SHADOW;
        }
    }

    public static class AdvancedSettings {
        public int cpuRenderAheadLimit = 3;
    }

    public static class QualitySettings {
        public GraphicsQuality weatherQuality;
        public GraphicsQuality leavesQuality;
        public boolean enableVignette;

        public QualitySettings() {
            this.weatherQuality = SodiumGameOptions.GraphicsQuality.DEFAULT;
            this.leavesQuality = SodiumGameOptions.GraphicsQuality.DEFAULT;
            this.enableVignette = true;
        }
    }

    public static class NotificationSettings {
        public boolean forceDisableDonationPrompts = false;
        public boolean hasClearedDonationButton = false;
        public boolean hasSeenDonationPrompt = false;
    }

    public enum GraphicsQuality implements TextProvider {
        DEFAULT("generator.default"),
        FANCY("options.clouds.fancy"),
        FAST("options.clouds.fast");

        private final ITextComponent name;

        GraphicsQuality(String name) {
            this.name = ComponentUtil.translatable(name);
        }

        public ITextComponent getLocalizedName() {
            return this.name;
        }

        public boolean isFancy(boolean fancyGraphics) {
            return this == FANCY || this == DEFAULT && fancyGraphics;
        }

        public boolean isFancy() {
            return this.isFancy(Minecraft.getMinecraft().gameSettings.fancyGraphics);
        }
    }
}
