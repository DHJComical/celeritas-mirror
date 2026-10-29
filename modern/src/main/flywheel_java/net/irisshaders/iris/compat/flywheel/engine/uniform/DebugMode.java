package net.irisshaders.iris.compat.flywheel.engine.uniform;

import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum DebugMode implements StringRepresentable
{
    OFF,
    NORMALS,
    TANGENTS,
    TANGENTS_HANDEDNESS,
    BLOCK_ID,
    MIDTEX,
    MIDMESH,
    EMISSION,
    INSTANCE_ID,
    LIGHT_LEVEL,
    OVERLAY,
    OLD_LIGHTING,
    MODEL_ID;

    public static final Codec<DebugMode> CODEC = StringRepresentable.fromEnum(DebugMode::values);

    private DebugMode() {
    }

    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static class CommandArgument extends StringRepresentableArgument<DebugMode>
    {
        public static final CommandArgument INSTANCE = new CommandArgument();
        public static final SingletonArgumentInfo<CommandArgument> INFO = SingletonArgumentInfo.contextFree(() -> INSTANCE);

        public CommandArgument()
        {
            super(DebugMode.CODEC, DebugMode::values);
        }
    }
}
