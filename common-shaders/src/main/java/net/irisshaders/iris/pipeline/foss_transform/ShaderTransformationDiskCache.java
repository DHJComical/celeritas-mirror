package net.irisshaders.iris.pipeline.foss_transform;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

public class ShaderTransformationDiskCache {
    /**
     * This value must be incremented whenever a new version of Cornea is published with updated transformers.
     */
    private static final int TRANSFORMER_VERSION = 1;

    private static final Path SHADER_CACHE_PATH = PLATFORM_UTIL.getGameDir().resolve("cornea_transform_cache");

    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<PatchShaderType, String> transformIfAbsent(ShaderTransformer.TransformKey<?> tKey, Supplier<Map<PatchShaderType, String>> transformFn) {
        // TODO - this will be hard to remember to invalidate, disabling disk caching for now
        if (true || !ShaderTransformer.useCache) {
            return transformFn.get();
        }
        String data = "taumc_glt_v" + TRANSFORMER_VERSION + new Gson().toJson(tKey);
        DIGEST.reset();
        byte[] hash = DIGEST.digest(data.getBytes(StandardCharsets.UTF_8));
        String hashFileName = Hex.encodeHexString(hash) + ".dat";
        Path path = SHADER_CACHE_PATH.resolve(hashFileName);
        TypeToken<Map<PatchShaderType, String>> typeToken = new TypeToken<>() {};
        try (Reader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(Files.newInputStream(path))))) {
            Map<PatchShaderType, String> map = new Gson().fromJson(reader, typeToken.getType());
            if(map != null && !map.isEmpty()) {
                return map;
            } else {
                IRIS_LOGGER.error("Cache data is corrupt");
                Files.deleteIfExists(path);
            }
        } catch(FileNotFoundException | NoSuchFileException ignored) {
        } catch(IOException e) {
            IRIS_LOGGER.error("Error loading transformed shader, will re-transform now", e);
        }

        Map<PatchShaderType, String> results = transformFn.get();

        try {
            Files.createDirectories(SHADER_CACHE_PATH);
            try(Writer writer = new BufferedWriter(new OutputStreamWriter(new DeflaterOutputStream(Files.newOutputStream(path))))) {
                writer.write(new Gson().toJson(results, typeToken.getType()));
            }
        } catch(IOException e) {
            IRIS_LOGGER.error("Error writing transformed shader to disk", e);
        }

        return results;
    }
}
