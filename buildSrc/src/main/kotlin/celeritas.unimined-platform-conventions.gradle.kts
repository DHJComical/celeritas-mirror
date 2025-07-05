import org.embeddedt.embeddium.gradle.build.extensions.CeleritasExtension

plugins {
    id("xyz.wagyourtail.unimined")
}

extensions.create("celeritas", CeleritasExtension::class)