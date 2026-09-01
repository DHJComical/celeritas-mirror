package org.embeddedt.embeddium.gradle.build.extensions

import bs.ModLoader
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

fun Project.versionedProperty(name: String): String? {
    return rootProject.findProperty("${name}_${ModLoader.getMinecraftVersion(project).replace('.', '_')}")?.toString()
}

/**
 * The mutable list of mixin configs that end up in the mod jar's manifest. Seeded by
 * `celeritas.platform-conventions` and appended to by the platform build scripts.
 *
 * `extra` is untyped, so the cast is unavoidable; keeping it here means the call sites don't
 * each have to repeat (and suppress) it.
 */
@Suppress("UNCHECKED_CAST")
val Project.celeritasMixinConfigs: MutableList<String>
    get() = extra.get("celeritasMixinConfigs") as MutableList<String>
