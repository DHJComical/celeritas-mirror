package org.embeddedt.embeddium.gradle.build.extensions

import bs.ModLoader
import org.gradle.api.Project

fun Project.versionedProperty(name: String): String? {
    return rootProject.properties.getValue("${name}_${ModLoader.getMinecraftVersion(project).replace('.', '_')}")?.toString()
}