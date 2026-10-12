# NexIDE adaptations of the NIXEND IDE Skills Kit

This document maps the generic IntelliJ-analog kit onto **this** repository
(`NexIDE` — Compose Desktop, custom `platform/*`, not JetBrains Platform).

## Source of truth

| Kit artifact | NexIDE mapping |
|--------------|----------------|
| Coding style (2 spaces, 140 cols, `my`/`our`, Apache 2.0) | `.editorconfig` + kit `standards/INTELLIJ_PLATFORM_CODING_GUIDELINES_1.0.md` |
| Review checklist | `standards/REVIEW_CHECKLIST.md` (use as-is for style/diff hygiene) |
| Build / tests against `intellij-community` | **Not applicable.** Use Gradle modules below |
| OpenAPI / `@ApiStatus.Experimental` | Optional for future public plugin API; internal APIs need KDoc when non-obvious |

## Build & test commands (replace intellij-community)

```bash
./gradlew compileKotlin
./gradlew :platform:core:test :platform:editor:test :ide-app:test
./gradlew :ide-app:run
```

CI: `.github/workflows/ci.yml` (JDK 21, compile + unit tests).

## Module map

| Gradle module | Role |
|---------------|------|
| `platform:util` | Logging, shared helpers |
| `platform:core` | Project, VFS, Gradle model, settings, index, run/debug |
| `platform:psi` | Lightweight structure / PSI-ish |
| `platform:plugin` | Extension points |
| `platform:editor` | Document, syntax, editor UI pieces |
| `platform:ui` | Compose tool windows / dialogs |
| `ide-app` | `Main`, shell actions, menus, session |

## Agent protocol for NexIDE

1. Read `AGENTS.md`, this file, and the relevant `skills/*/SKILL.md`.
2. Prefer small focused commits; do not mix feature + mass reformat.
3. Do not invent Compose / Gradle / JDI APIs — verify in tree.
4. After shell/UI wiring changes, sanity-check `Main.kt` brace balance and `IdeMenuBar` presence.
5. Report honestly if sandbox cannot run Gradle (timeouts, missing display).

## Skills priority for daily work

| Skill | When |
|-------|------|
| `nixend-developer` | Implementing a feature |
| `nixend-architect` | Multi-module or shell extraction |
| `nixend-code-audit` | Before merge / after large Main edits |
| `nixend-build-ci` | CI / test wiring |
| `nixend-testing` | New unit tests |
| `nixend-release` | Version bump (`NexIdeVersion`, README, packageVersion) |

## Version bump checklist

1. `platform/core/.../NexIdeVersion.kt`
2. `README.md` status line
3. `ide-app/build.gradle.kts` → `packageVersion`
