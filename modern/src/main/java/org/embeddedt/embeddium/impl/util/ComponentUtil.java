package org.embeddedt.embeddium.impl.util;

import net.minecraft.network.chat.Component;
//? if >=1.16
import net.minecraft.network.chat.MutableComponent;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}

//? if >=1.19 {
public class ComponentUtil {
    public static MutableComponent empty() {
        return Component.empty();
    }

    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
//?} else if >=1.16 {
/*public class ComponentUtil {
    public static MutableComponent empty() {
        return new TextComponent("");
    }

    public static MutableComponent literal(String text) {
        return new TextComponent(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return new TranslatableComponent(key, args);
    }
}
*///?} else {
/*import net.minecraft.network.chat.BaseComponent;

public class ComponentUtil {
    public static BaseComponent empty() {
        return new TextComponent("");
    }

    public static BaseComponent literal(String text) {
        return new TextComponent(text);
    }

    public static BaseComponent translatable(String key, Object... args) {
        return new TranslatableComponent(key, args);
    }
}
*///?}
