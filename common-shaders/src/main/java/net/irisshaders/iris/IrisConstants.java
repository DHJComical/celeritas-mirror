package net.irisshaders.iris;

public class IrisConstants {
    public static final String MODID = "embeddium";
    /**
     * The user-facing name of the mod. Moved into a constant to facilitate
     * easy branding changes (for forks). You'll still need to change this
     * separately in mixin plugin classes & the language files.
     */
    public static final String MODNAME = "Celeritas";
    public static final float DEPTH = 0.125F;

    // Bump this up if you want more shadow color buffers!
    // This is currently set at 2 for ShadersMod / OptiFine parity but can theoretically be bumped up to 8.
    // TODO: Make this configurable?
    public static final int MAX_SHADOW_COLOR_BUFFERS_IRIS = 8;
    public static final int MAX_SHADOW_COLOR_BUFFERS_OF = 2;
}
