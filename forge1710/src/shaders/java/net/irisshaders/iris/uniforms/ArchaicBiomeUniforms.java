package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.FloatSupplier;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;


public class ArchaicBiomeUniforms implements BiomeUniforms {
    @Override
    public void addBiomeUniforms(UniformHolder uniforms) {
        // All based on player's location
        // "biome", int, BiomeID
        // "biome_category", int, BiomeCategory - BiomeDictionary.Type should be usable
        // "biome_precipitation", int, Current Precipitation -- seems enableSnow helps with this
        // "rainfall", float, rainfall
        // "temperature", float, temperature

    }

    static IntSupplier playerI(ToIntFunction<EntityPlayer> function) {
        return () -> {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null) {
                return 0; // TODO: I'm not sure what I'm supposed to do here?
            } else {
                return function.applyAsInt(player);
            }
        };
    }

    static FloatSupplier playerF(ToFloatFunction<EntityPlayer> function) {
        return () -> {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null) {
                return 0.0f; // TODO: I'm not sure what I'm supposed to do here?
            } else {
                return function.applyAsFloat(player);
            }
        };
    }

    @FunctionalInterface
    public interface ToFloatFunction<T> {
        /**
         * Applies this function to the given argument.
         *
         * @param value the function argument
         * @return the function result
         */
        float applyAsFloat(T value);
    }
}
