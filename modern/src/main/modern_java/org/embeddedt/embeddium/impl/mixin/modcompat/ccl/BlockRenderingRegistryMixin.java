package org.embeddedt.embeddium.impl.mixin.modcompat.ccl;

//? if neoforge && 1.21.1 {

/*import codechicken.lib.render.block.BlockRenderingRegistry;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockRenderingRegistry.class)
public class BlockRenderingRegistryMixin {
    /^*
     * @author embeddedt
     * @reason CCL moved the compat to their end. However, this relies on the assumption that we still implement the
     * Embeddium APIs... some of which we don't, as they got repackaged in the process of cleaning up code and creating
     * the common module. That means we need to provide the compat on our end still, while preventing CCL's from running.
     * There are two solutions to this problem: stop providing the 'embeddium' mod ID, or stop CCL from detecting
     * Embeddium. Since we currently have very few known mods that fail to run with Celeritas' limited Embeddium API
     * support, the latter option is likely to require less work for the time being.
     *
     * This mixin is only necessary for 1.21.1. In older versions, Embeddium provided the compat anyway, and after
     * 1.21.1 CCL is removing this difficult-to-support API entirely.
     ^/
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/ModList;isLoaded(Ljava/lang/String;)Z"), require = 0)
    private static boolean pretendWeAreNotEmbeddium(ModList instance, String modTarget) {
        if (modTarget.equals("embeddium")) {
            return false;
        } else {
            return instance.isLoaded(modTarget);
        }
    }
}

*///?}
