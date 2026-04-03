package org.embeddedt.embeddium.impl.modern.compat.ccl;

//? if codechickenlib {

import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.19 {
import net.minecraft.core.Holder;
//? if forge
import net.minecraftforge.registries.ForgeRegistries;
//?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?}
//? if <1.19
/*import net.minecraftforge.registries.IRegistryDelegate;*/
//? if neoforge {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
*///?}
import org.embeddedt.embeddium.api.BlockRendererRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CCLCompat {
    //? if forge && >=1.19 {
	private static Map<Holder<Block>, ICCBlockRenderer> customBlockRenderers;
    private static Map<Holder<Fluid>, ICCBlockRenderer> customFluidRenderers;
    //?} else if forge && <1.19 {
    /*private static Map<IRegistryDelegate<Block>, ICCBlockRenderer> customBlockRenderers;
    private static Map<IRegistryDelegate<Fluid>, ICCBlockRenderer> customFluidRenderers;
    *///?} else if neoforge {
    /*private static Map<Block, ICCBlockRenderer> customBlockRenderers;
    private static Map<Fluid, ICCBlockRenderer> customFluidRenderers;
    *///?}
    private static List<ICCBlockRenderer> customGlobalRenderers;

    private static final Map<ICCBlockRenderer, BlockRendererRegistry.Renderer> ccRendererToSodium = new ConcurrentHashMap<>();
    private static final ThreadLocal<PoseStack> STACK_THREAD_LOCAL = ThreadLocal.withInitial(PoseStack::new);

    /**
     * Wrap a CodeChickenLib renderer in Embeddium's API.
     */
    private static BlockRendererRegistry.Renderer createBridge(ICCBlockRenderer r) {
        return ccRendererToSodium.computeIfAbsent(r, ccRenderer -> (ctx, random, consumer) -> {
            ccRenderer.renderBlock(ctx.state(), ctx.pos(), ctx.localSlice(), STACK_THREAD_LOCAL.get(), consumer, random, ctx.modelData()/*? if >=1.19 {*/, ctx.renderLayer()/*?}*/);
            return BlockRendererRegistry.RenderResult.OVERRIDE;
        });
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if(ModList.get().isLoaded("codechickenlib")) {
            init();
            BlockRendererRegistry.instance().registerRenderPopulator((resultList, ctx) -> {
                if(!customGlobalRenderers.isEmpty()) {
                    for(ICCBlockRenderer r : customGlobalRenderers) {
                        if(r.canHandleBlock(ctx.localSlice(), ctx.pos(), ctx.state()/*? if >=1.19 {*/, ctx.renderLayer()/*?}*/)) {
                            resultList.add(createBridge(r));
                        }
                    }
                }
                if(!customBlockRenderers.isEmpty()) {
                    Block block = ctx.state().getBlock();
                    //? if neoforge {
                    /*var holder = block;
                    *///?} else if >=1.19 {
                    var holder = ForgeRegistries.BLOCKS.getDelegateOrThrow(block);
                    //?} else
                    /*var holder = block.delegate;*/
                    var renderer = customBlockRenderers.get(holder);
                    if (renderer != null && renderer.canHandleBlock(ctx.localSlice(), ctx.pos(), ctx.state()/*? if >=1.19 {*/, ctx.renderLayer()/*?}*/)) {
                        resultList.add(createBridge(renderer));
                    }
                }
                if(!customFluidRenderers.isEmpty()) {
                    Fluid fluid = ctx.state().getFluidState().getType();
                    //? if neoforge {
                    /*var holder = fluid;
                    *///?} else if >=1.19 {
                    var holder = ForgeRegistries.FLUIDS.getDelegateOrThrow(fluid);
                    //?} else
                    /*var holder = fluid.delegate;*/
                    var renderer = customFluidRenderers.get(holder);
                    if (renderer != null && renderer.canHandleBlock(ctx.localSlice(), ctx.pos(), ctx.state()/*? if >=1.19 {*/, ctx.renderLayer()/*?}*/)) {
                        resultList.add(createBridge(renderer));
                    }
                }
            });
        }
    }

    
	@SuppressWarnings({"rawtypes","unchecked"})
	public static void init() {
		try {
            final Field blockRenderersField = BlockRenderingRegistry.class.getDeclaredField("blockRenderers");
            blockRenderersField.setAccessible(true);
            customBlockRenderers = (Map)blockRenderersField.get(null);

            final Field fluidRenderersField = BlockRenderingRegistry.class.getDeclaredField("fluidRenderers");
            fluidRenderersField.setAccessible(true);
            customFluidRenderers = (Map)fluidRenderersField.get(null);

            final Field globalRenderersField = BlockRenderingRegistry.class.getDeclaredField("globalRenderers");
            globalRenderersField.setAccessible(true);
            customGlobalRenderers = (List<ICCBlockRenderer>) globalRenderersField.get(null);

            if(customBlockRenderers == null)
                customBlockRenderers = Collections.emptyMap();
            if(customFluidRenderers == null)
                customFluidRenderers = Collections.emptyMap();
            if(customGlobalRenderers == null)
                customGlobalRenderers = Collections.emptyList();
        }
        catch (final @NotNull Throwable t) {
        	t.printStackTrace();
        }
	}
	
}

//?}