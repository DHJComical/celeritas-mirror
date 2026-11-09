package net.irisshaders.iris.modern.materialmap;

import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import org.jetbrains.annotations.Nullable;

/**
 * A vanilla block shape that materials commonly come in, identified by the vanilla block class. The keywords are the
 * registry name forms that mods use for that shape, matched as a {@code _keyword} suffix or {@code keyword_} prefix.
 */
public enum ShapeVariant {
    SLAB(SlabBlock.class, "slab"),
    STAIRS(StairBlock.class, "stairs", "stair"),
    WALL(WallBlock.class, "wall"),
    FENCE(FenceBlock.class, "fence"),
    FENCE_GATE(FenceGateBlock.class, "fence_gate", "gate"),
    DOOR(DoorBlock.class, "door"),
    TRAPDOOR(TrapDoorBlock.class, "trapdoor", "trap_door"),
    BUTTON(ButtonBlock.class, "button"),
    PRESSURE_PLATE(BasePressurePlateBlock.class, "pressure_plate", "plate"),
    PANE(IronBarsBlock.class, "pane");

    private static final ShapeVariant[] VALUES = values();

    private final Class<? extends Block> blockClass;
    final String[] keywords;

    ShapeVariant(Class<? extends Block> blockClass, String... keywords) {
        this.blockClass = blockClass;
        this.keywords = keywords;
    }

    public static @Nullable ShapeVariant of(Block block) {
        for (ShapeVariant variant : VALUES) {
            if (variant.blockClass.isInstance(block)) {
                return variant;
            }
        }
        return null;
    }
}
