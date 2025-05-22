package org.embeddedt.embeddium.gradle.build.extensions

import org.gradle.api.Project

val Project.celeritas
    get() = extensions.getByType(CeleritasExtension::class.java)

open class CeleritasExtension(val project: Project) {

}