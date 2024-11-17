<img src="src/main/resources/icon.png" width="128">

# Celeritas

Celeritas is a free and open-source performance mod for Minecraft clients. It is a fork of Embeddium, which itself
was based on the last FOSS-licensed version of Sodium.

I maintain this mod for personal use/experimentation and make the source code available for others who
might be interested. I have no plans to release binaries on CurseForge or Modrinth, and I provide no
guarantee of active maintenance or bugfixes. That said, the code remains
LGPL-3.0, so other projects under a compatible license (including Embeddium) should feel free to incorporate bugfixes
and features they find useful.


## How to build

Celeritas uses [Stonecutter](https://github.com/stonecutter-versioning/stonecutter) toolchain to target many versions
of Minecraft simultaneously. `./gradlew chiseledPackage` can be used to compile the mod for all of the supported targets.

## License

Celeritas is licensed under the Lesser GNU General Public License version 3.

Portions of the option screen code are based on Reese's Sodium Options by FlashyReese, and are used under the terms of
the [MIT license](https://opensource.org/license/mit), located in `src/main/resources/licenses/rso.txt`. 
