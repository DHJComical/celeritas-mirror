<img src="modern/src/main/resources/icon.png" width="128">

# Celeritas

Celeritas is a free and open-source performance & shaders mod for Minecraft clients. It is a fork of Embeddium (which itself
was based on the last FOSS-licensed version of Sodium) and Oculus 1.7.

I maintain this mod for personal use & experimentation and make the source code available for other projects and
developers who may be interested. There is also no guarantee of active maintenance, including bugfixes
or ports to any newer Minecraft versions. That said, the code remains
LGPL-3.0, so other projects under a compatible license (including Embeddium) should feel free to incorporate bugfixes
and features they find useful. That said, expect minimal support, and many possible bugs due to limited testing.

**Important note:** There are currently no official Celeritas binary releases. If you download a precompiled
Celeritas .jar file from any 3rd party source, we cannot provide any support for such files, and you do so at your own
risk. As of writing, the only official distribution of Celeritas available is the original source code
at https://git.taumc.org/embeddedt/celeritas, 

## Project layout

Celeritas uses the [Stonecutter](https://codeberg.org/stonecutter/stonecutter) toolchain to reduce the effort required
to support individual Minecraft versions. Additionally, as much core rendering code as possible is fully abstracted
from Minecraft within a `:common` project. The common module is published on
[Maven](https://maven.taumc.org/#/releases/org/embeddedt/celeritas/celeritas-common), to allow downstream projects to
consume it without rebuilding the entire project from source. However, the production mod jars are not available on Maven.

## How to build

The fastest way to build for exactly one version target is to run `./gradlew -Ptarget_versions=<version> packageJar`.
This command avoids configuring as many Minecraft targets as possible. The resulting jar file will be available
in `build/libs/<celeritas version>`.

Note: the `target_versions` property accepts a standard Stonecutter predicate, so you can also use syntax like
`./gradlew -Ptarget_versions="<1.8.9"`.

You can also explicitly target a project with regular Gradle syntax, e.g. `./gradlew :forge122:1.12.2:packageJar`
or `./gradlew :forge1710:packageJar`, but this will configure other subprojects even if they're not used.

To build for every Minecraft version at once, execute `./gradlew packageJar`.

## How to use

Celeritas generally requires a "modernized" environment on older Minecraft versions, and will not run out-of-the-box
with a default modded Minecraft instance. Newer Minecraft versions ship with the necessary dependencies and will not
require any custom setup.

For legacy (pre-1.13) versions, your game instance must provide LWJGL 3, and in some cases must also be running Java 21.
Most of these versions do not have a ready-to-download launcher profile available that meets these requirements out of
the box, except for Forge 1.7.10 (lwjgl3ify) and Forge 1.12.2 (lwjgl3ify or Cleanroom Loader).

The Ornithe versions (pre-1.7) are especially experimental and have not yet been tested outside of a development environment
at all.

For modern (1.13+) versions, the final mod jar should run as-is in a standard instance for that version (e.g. Java 17
or 21 are not required, unless the underlying Minecraft version itself requires them).

## License

Celeritas is licensed under the Lesser GNU General Public License version 3, as it only uses code from Iris 1.7,
Sodium 0.5.11-, and other FOSS projects.

Portions of the option screen code are based on Reese's Sodium Options by FlashyReese, and are used under the terms of
the [MIT license](https://opensource.org/license/mit), located in `src/main/resources/licenses/rso.txt`.

This project does not include and has no plans to include any code from Sodium 0.6+ or 0.5.12+, as these versions of
Sodium are not available under a free and open-source license.
Please reach out to @embeddedt on Discord if you have concerns regarding the license of any code in this project.

## Credits

* The CaffeineMC team, for developing Sodium 0.5.11 & older, and making it open source
* Asek3, for developing Rubidium, the original port of Sodium 0.5 to Forge
* CelestialAbyss, for developing the Embeddium logo (which is reused here aside from recoloring), and input-Here for some very good visual touchups
* Ven ([@basdxz](https://github.com/basdxz)), for help with translucency sorting, suggesting the general approach for async occlusion culling, and other suggestions during development
* XFactHD, Pepper, and anyone else I've forgotten to mention, for providing valuable code insights

[![YourKit logo](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.

Special thanks to YourKit for providing a free license for my various open-source Minecraft projects.