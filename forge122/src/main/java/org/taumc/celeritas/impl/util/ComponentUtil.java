package org.taumc.celeritas.impl.util;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

public class ComponentUtil {

    public static ITextComponent empty() {
        return new TextComponentString("");
    }

    public static ITextComponent literal(String text) {
        return new TextComponentString(text);
    }

    public static ITextComponent translatable(String key, Object... args) {
        return new TextComponentTranslation(key, args);
    }
}
