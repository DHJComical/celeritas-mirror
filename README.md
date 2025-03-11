<img src="modern/src/main/resources/icon.png" width="128">

# Celeritas

Celeritas is a free and open-source performance & shaders mod for Minecraft clients. It is a fork of Embeddium (which itself
was based on the last FOSS-licensed version of Sodium) and Oculus 1.7.

I maintain this mod for personal use & experimentation and make the source code available for other projects and
developers who may be interested. There are currently no plans to distribute binaries for modern Minecraft versions on CurseForge,
Modrinth, or any other player-facing mod platform. There is also no guarantee of active maintenance, including bugfixes
or ports to any newer Minecraft versions. That said, the code remains
LGPL-3.0, so other projects under a compatible license (including Embeddium) should feel free to incorporate bugfixes
and features they find useful. That said, expect minimal support, and many possible bugs due to limited testing.

## How to build

### Modern

Celeritas uses [Stonecutter](https://github.com/stonecutter-versioning/stonecutter) toolchain to target many versions
of modern (post-14) Minecraft simultaneously. `./gradlew chiseledPackage` can be used to compile the mod for all of the supported targets.

To build for a specific version of modern Minecraft, use `./scripts/build.sh <mcversion>-<loader>` (e.g. `./scripts/build.sh 1.21.1-neoforge`).
This will fail if the given version/loader combo is not supported.

### Vintage

Currently the only vintage version of Minecraft supported is 12.2, in a dedicated `:forge122` project.

### Archaic

Currently the only archaic version of Minecraft supported is 7.10, in a dedicated `:forge1710` project.

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