# Celeritas

This is the source tree for Celeritas, a fork of Embeddium and Oculus/Iris. It supports many Minecraft
versions and loaders from one tree, using Stonecutter. Rendering code that doesn't depend on
Minecraft lives in `:common`.

## Layout

- `common/`: shared rendering core (`org.embeddedt.embeddium.impl.*`). New Celeritas-owned API goes
  in `org.taumc.celeritas`. The build script is Groovy (`common/build.gradle`). JMH benches are in
  `common/src/jmh`, tests in `common/src/test`.
- `modern/`: the Stonecutter tree for 1.16.5 to 26.x. Sources live in `modern/src/main/<set>`. The
  build scripts in `buildSrc/src/main/kotlin/celeritas.*-conventions.gradle.kts` choose which source
  sets each target compiles:
  - `java`: always compiled.
  - `modern_java`: pre-26.1.
  - `postmodern_java`: 26.1 and later.
  - `shaders_java` and `batching_java`: the Iris fork, shared by every shader-enabled target.
  - `shaders_modern_java` and `shaders_postmodern_java`: Iris code for one side of the 26.1 split.
- `forge122/` (RetroFuturaGradle), `forge1710/` (RFG) and `ornithe/` (Loom/Ploceus): the legacy
  loaders. They have their own copies of some classes that `modern/` also has, so a fix may need to
  go in more than one place.
- The repo root has no `src/`. Always use module-prefixed paths.
- Version and dependency pins live in the root `gradle.properties`, keyed by version suffix.
- `modern/versions/<mc>-<loader>/build/generated/stonecutter/` holds generated output. Read it to see
  what one target actually compiles, but never edit it. Many directories under `modern/versions/` are
  stale leftovers. Only the targets listed in `settings.gradle.kts` are real.

## Building and verifying

- Use qualified paths from the repo root, like `./gradlew :modern:1.20.1-forge:compileJava` or
  `:common:compileJava`. Select targets with `-Pceleritas_target_versions=<mc>`.
- Builds are slow and memory-hungry, so run them one at a time.
- Compiling the active target (`1.20.1-forge`) doesn't check other version branches. After editing
  shared or version-gated code, also compile a target on the other side of the condition.
- `:common` must stay compatible with fastutil 7.1.0.
- JMH benches: `./gradlew :common:jmh -Pjmhargs="..."`.
- Never run `taugradle_publish`, because it publishes artifacts.

## Licensing

- Never read, port or cherry-pick Sodium 0.6+ code, or commits that came from that lineage. Its
  license is incompatible and would contaminate our codebase. Don't pull upstream Iris changes
  that clearly depend on or derive from newer Sodium either.
- Celeritas has no addon API. Don't add compat shims or feature flags to keep third-party addons
  working.
