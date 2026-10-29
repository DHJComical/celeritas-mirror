package net.irisshaders.iris.compat.flywheel;

import net.irisshaders.iris.compat.flywheel.engine.ClrwlEngine;
import net.irisshaders.iris.compat.flywheel.indirect.ClrwlBufferBindings;
import net.irisshaders.iris.compat.flywheel.indirect.ClrwlIndirectDrawManager;
import net.irisshaders.iris.compat.flywheel.instancing.ClrwlInstancedDrawManager;
import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.backend.gl.Driver;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.lib.backend.SimpleBackend;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.sampler.SamplerLimits;
import net.irisshaders.iris.shaderpack.ShaderPack;

import java.util.Optional;

public class ClrwlBackend
{
    public static final Backend IRIS_INSTANCING = SimpleBackend.builder()
            .engineFactory(level -> new ClrwlEngine(level, 256, ClrwlInstancedDrawManager::build))
            .priority(500)
            .supported(() -> GlCompat.SUPPORTS_INSTANCING && isUsingCompatibleShaderPack())
            .register(Colorwheel.rl("instancing"));

    public static final Backend IRIS_INDIRECT = SimpleBackend.builder()
            .engineFactory(level -> new ClrwlEngine(level, 256, ClrwlIndirectDrawManager::build))
            .priority(() -> {
                // Read from GlCompat in these provider because loading GlCompat
                // at the same time the backends are registered causes GlCapabilities to be null.
                if (GlCompat.DRIVER == Driver.INTEL)
                {
                    // Intel has very poor performance with indirect rendering, and on top of that has graphics bugs
                    return 1;
                }
                else
                {
                    return 1000;
                }
            })
            .supported(() -> GlCompat.SUPPORTS_INDIRECT && isUsingCompatibleShaderPack() && hasEnoughShaderStorageUnits())
            .register(Colorwheel.rl("indirect"));

    private static ShaderPack warnedIncompatiblePack;

    // Set when rendering with the current pack failed. Reloading creates a new ShaderPack, which retries.
    private static ShaderPack failedPack;

    public static void init()
    {
        ClrwlRenderPasses.init();
    }

    /**
     * Stops Flywheel from selecting these backends again until the shader pack is reloaded, so a pack that fails to
     * render can't make every level renderer reload fail the same way.
     */
    public static void disableForCurrentPack()
    {
        failedPack = Iris.getCurrentPack().orElse(null);
        Colorwheel.LOGGER.error("Disabling the Flywheel backend for shader pack {} until it is reloaded", Iris.getCurrentPackName());
    }

    public static boolean isCurrentBackend(Backend backend)
    {
        return backend == IRIS_INSTANCING || backend == IRIS_INDIRECT;
    }

    public static boolean isUsingCompatibleShaderPack()
    {
        Optional<ShaderPack> pack = Iris.getCurrentPack();

        if (pack.isEmpty() || pack.get() == failedPack)
        {
            return false;
        }

        var programs = pack.get().getProgramSet(Iris.getCurrentDimension()).getClrwlPrograms();

        if (programs == null)
        {
            return false;
        }

        if (programs.isFallbackMode() && !Iris.getIrisConfig().isFlywheelFallbackMode())
        {
            if (warnedIncompatiblePack != pack.get())
            {
                warnedIncompatiblePack = pack.get();
                Colorwheel.LOGGER.warn("Shader pack {} has no Flywheel programs; enable flywheelFallbackMode in the Iris config to render Flywheel content through its gbuffers programs",
                        Iris.getCurrentPackName());
            }

            return false;
        }

        return true;
    }

    /**
     * @return the first SSBO binding above every binding the pack declares
     */
    public static int getShaderStorageBase(ShaderPack pack)
    {
        var irisBufferObjects = pack.getShaderProperties().getBufferObjects();

        return irisBufferObjects.isEmpty() ? 0 : irisBufferObjects.keySet().intStream().max().getAsInt() + 1;
    }

    public static boolean hasEnoughShaderStorageUnits()
    {
        Optional<ShaderPack> pack = Iris.getCurrentPack();

        if (pack.isEmpty())
        {
            return false;
        }

        int base = getShaderStorageBase(pack.get());
        int maxUnits = SamplerLimits.get().getMaxShaderStorageUnits();
        boolean hasEnoughUnits = base + ClrwlBufferBindings.TOTAL_BINDING_COUNT <= maxUnits;

        if (!hasEnoughUnits)
        {
            Colorwheel.LOGGER.error("Graphics card doesn't have enough storage units: {} + {} > {}", base, ClrwlBufferBindings.TOTAL_BINDING_COUNT, maxUnits);
        }

        return hasEnoughUnits;
    }
}
