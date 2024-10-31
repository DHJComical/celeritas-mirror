package org.embeddedt.embeddium.impl.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}

public class ComponentUtil {
    public static MutableComponent empty() {
        //? if >=1.19 {
        return Component.empty();
        //?} else
        /*return new TextComponent("");*/
    }

    public static MutableComponent literal(String text) {
        //? if >=1.19 {
        return Component.literal(text);
        //?} else
        /*return new TextComponent(text);*/
    }

    public static MutableComponent translatable(String key, Object... args) {
        //? if >=1.19 {
        return Component.translatable(key, args);
        //?} else
        /*return new TranslatableComponent(key, args);*/
    }
}
