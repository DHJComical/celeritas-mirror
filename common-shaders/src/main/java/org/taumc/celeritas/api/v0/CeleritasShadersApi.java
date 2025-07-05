package org.taumc.celeritas.api.v0;

import net.irisshaders.iris.api.v0.IrisApiConfig;
import java.util.ServiceLoader;

public interface CeleritasShadersApi {
    CeleritasShadersApi INSTANCE = ServiceLoader.load(CeleritasShadersApi.class).findFirst().orElseThrow();

    /**
     * @since API v0.0
     */
    static CeleritasShadersApi getInstance() {
        return INSTANCE;
    }


    /**
     * Checks whether a shader pack is currently in use and being used
     * for rendering. If there is no shader pack enabled or a shader
     * pack failed to compile and is therefore not in use, this will
     * return false.
     *
     * <p>Mods that need to enable custom workarounds for shaders
     * should use this method.
     *
     * @return Whether shaders are being used for rendering.
     * @since {@link #getMinorApiRevision() API v0.0}
     */
    boolean isShaderPackInUse();

    /**
     * Checks whether the shadow pass is currently being rendered.
     *
     * <p>Generally, mods won't need to call this function for much.
     * Mods should be fine with things being rendered multiple times
     * each frame from different camera perspectives. Often, there's
     * a better approach to fixing bugs than calling this function.
     *
     * <p>Pretty much the main legitimate use for this function that
     * I've seen is in a mod like Immersive Portals, where it has
     * very custom culling that doesn't work when the Iris shadow
     * pass is active.
     *
     * <p>Naturally, this function can only return true if
     * {@link #isShaderPackInUse()} returns true.
     *
     * @return Whether Iris is currently rendering the shadow pass.
     * @since API v0.0
     */
    boolean isRenderingShadowPass();

    /**
     * Gets the shadow distance that should be used for rendering shadows.
     * This method is used to override the shadow distance set in the
     * Minecraft options screen.
     *
     * @param base The shadow distance set in the Minecraft options screen.
     * @return The shadow distance that should be used for rendering shadows.
     * @since API v0.2
     */
    int getOverriddenShadowDistance(int base);

    /**
     * Checks whether the shadow distance slider should be enabled in the
     * Minecraft options screen. If this method returns false, the shadow
     * distance slider should be disabled.
     *
     * @return Whether the shadow distance slider should be enabled.
     * @since API v0.2
     */
    boolean isShadowDistanceSliderEnabled();

    /**
     * Checks whether debug options are enabled. Debug options give much
     * more detailed OpenGL error outputs at the cost of performance.
     *
     * @return Whether debug options are enabled.
     * @since API v0.2
     */
    boolean areDebugOptionsEnabled();

    /**
     * Opens the main Iris GUI screen. It's up to Iris to decide
     * what this screen is, but generally this is the shader selection
     * screen.
     * <p>
     * This method takes and returns Objects instead of any concrete
     * Minecraft screen class to avoid referencing Minecraft classes.
     * Nevertheless, the passed parent must either be null, or an
     * object that is a subclass of the appropriate {@code Screen}
     * class for the given Minecraft version.
     *
     * @param parent The parent screen, an instance of the appropriate
     *               {@code Screen} class.
     * @return A {@code Screen} class for the main Iris GUI screen.
     * @since API v0.0
     */
    Object openMainIrisScreenObj(Object parent);

    /**
     * Gets the language key of the main screen. Currently, this
     * is "options.iris.shaderPackSelection".
     *
     * @return the language key, for use with {@code TranslatableText}
     * / {@code TranslatableComponent}
     * @since API v0.0
     */
    String getMainScreenLanguageKey();

    /**
     * Gets a config object that can edit the Iris configuration.
     *
     * @since API v0.0
     */
    IrisApiConfig getConfig();

    /**
     * Gets the sun path rotation used by the current shader pack.
     *
     * @return The sun path rotation as specified by the shader pack, or 0 if no shader pack is in use.
     * @since API v0.2
     */
    float getSunPathRotation();
}
