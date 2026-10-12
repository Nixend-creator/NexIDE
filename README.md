# NexIDE

**Modern IDE inspired by IntelliJ IDEA**


NexIDE is a full-featured desktop IDE built with Kotlin and Compose Multiplatform.
It aims to provide a powerful editing experience with PSI, plugin system, debugger and more.

## Status

🚧 **v0.2.2-SNAPSHOT** — usable MVP desktop IDE (text-heuristic analysis, not a full compiler front-end)

### Editor & UX
- Multi-tab editor, pin tabs, split view, soft wrap, zoom
- Syntax highlight (Kotlin/Java), brace match, auto-pair
- Line ops: comment, duplicate, move, delete, join, case
- Find / Replace, Find in Files, Go to Line
- Command Palette, Search Everywhere, Recent Files
- Bookmarks (F11), status bar (Ln/Col, git branch, problem count, **module**)
- Breadcrumbs with **Gradle module** prefix

### Project & VFS
- Open project, recent projects, session restore (tabs/theme/split)
- Project View + Structure + breadcrumbs
- Symbol index (single-file reindex on Save, full refresh after Build)
- Window title: project + **[:module]** + dirty marker (`•` / `•N`)
- **Confirm exit** on unsaved tabs (Settings → toggleable; Esc cancels)

### Gradle multi-module
- Lightweight model from `settings.gradle(.kts)` (`GradleProjectModel`)
- Structure: Modules list, current module highlight, **Build / Test / build.gradle**
- Palette: **Go to Module**, **Build/Test Module**, **Open build.gradle** (per module)
- Menus: Build Module, Run Module Tests, Reload Gradle Project
- Open settings / root build / `gradle.properties` / version catalog
- **Auto-reload** model after Save of settings/build/properties/catalog
- Runtime classpath resolver (cached, multi-module merge)

### Build / Run / Debug
- Gradle build & **test** (failures → Problems; module-scoped tasks)
- Run Configurations (Gradle / Application)
- Debug: JDWP, breakpoints, variables, watches, evaluate, jump to source

### Git
- Status badges, stage/commit, push/pull
- Diff gutter + **side-by-side / unified** diff

### Analysis & refactor (lightweight)
- Live diagnostics: braces/strings, unused import, missing import (index)
- Problems panel = build + live (debounced), filter All / Errors / Warnings
- Optimize Imports (Ctrl+Alt+O), Alt+Enter intentions (+ popup)
- Rename (preview, skips `//` comments), Reformat, Local History timeline

### Architecture (ide-app)
- `IdeSessionServices`, `IdeTabService`, `IdeNavigationService`, **`IdeModuleActions`**, **`IdeWindowTitle`**
- `IdeDialogHost`, `IdeKeyMap`, `IdeActionCatalog`, modular UI composables

### Tests
```bash
./gradlew :platform:core:test :platform:editor:test :ide-app:test
```

### Publish to GitHub
Checklist for creating the remote: [`docs/PUBLISH_GITHUB.md`](docs/PUBLISH_GITHUB.md).

### Engineering kit
Standards, agent skills, and review checklist: [`docs/engineering-kit/`](docs/engineering-kit/) (see **`NEXIDE.md`** for project-specific mappings).

### Continuous Integration
GitHub Actions (`.github/workflows/ci.yml`) runs on push/PR to `main`:
- `compileKotlin` for all modules
- unit tests: `platform:core`, `platform:editor`, `ide-app`

After publishing to GitHub, optional badge:
```markdown
![CI](https://github.com/<org>/NexIDE/actions/workflows/ci.yml/badge.svg)
```

## Run

```bash
# JDK 17+ (21 preferred when full JDK with ct.sym is available)
export JAVA_HOME=...
./gradlew :ide-app:run
```

### Tests

```bash
./gradlew :platform:editor:test --tests "com.nexide.platform.editor.*"
./gradlew :platform:core:test --tests "com.nexide.platform.core.index.*"
```

### Key shortcuts (window)

| Action | Shortcut |
|--------|----------|
| Open Project | Ctrl+O |
| Save / Save All | Ctrl+S / Ctrl+Shift+S |
| Recent Files | Ctrl+E |
| Find Action | Ctrl+Shift+A |
| Search Everywhere | Ctrl+N |
| Find in Files | Ctrl+Shift+F |
| Go to Declaration | Ctrl+B |
| Find Usages | Alt+F7 |
| Rename | Shift+F6 |
| Reformat | Ctrl+Alt+L |
| Optimize Imports | Ctrl+Alt+O |
| Intention / quick-fix | Alt+Enter |
| Next / Prev Problem | F2 / Shift+F2 |
| Build Project | Ctrl+F9 |
| Build / Test Module | Build menu or Palette (`Build Module :…`) |
| Reload Gradle Project | Build menu / Palette |
| Run / Debug | Shift+F10 / Shift+F9 |
| Toggle Bookmark | F11 |
| Next Bookmark | Shift+F11 |
| Zoom | Ctrl+= / − / 0 |
| Settings | Ctrl+Alt+S |

Palette also lists per-module actions: *Go to Module*, *Build Module*, *Test Module*, *Open build.gradle*.


## Tech Stack

- **Language:** Kotlin 2.4.20
- **UI:** Compose Multiplatform 1.12.1 (Desktop)
- **Build:** Gradle 9.8 + Version Catalog
- **JVM:** 21
- **License:** Apache 2.0

## Modules

| Module              | Description                              |
|---------------------|------------------------------------------|
| `platform:util`     | Shared utilities & logging               |
| `platform:core`     | Application, Project, core services      |
| `platform:psi`      | Program Structure Interface              |
| `platform:plugin`   | Plugin system & extension points         |
| `platform:editor`   | Editor component                         |
| `platform:ui`       | Compose UI components                    |
| `ide-app`           | Main application entry point             |

## Building

Requirements:
- JDK 21+
- Git

```bash
./gradlew :ide-app:run
```

## Coding Guidelines

We follow IntelliJ Platform inspired coding standards:

- Prefer Kotlin for all new code
- Indentation: 2 spaces
- Max line length: 140
- Instance fields: `my` prefix
- Static/class fields: `our` prefix
- Apache 2.0 copyright header in every source file
- See `.editorconfig` and full guidelines in the project

## Roadmap (high level)

1. ✅ Project skeleton + Compose Desktop bootstrap
2. ✅ Virtual File System (VFS) — LocalFileSystem + VirtualFile
3. ✅ Basic Editor (Document + Editor + Compose view)
4. ✅ PSI foundation (PsiElement / PsiFile / PsiManager)
5. ✅ Plugin system (Extension Points + PluginManager)
6. ✅ Project View (file tree)
7. ✅ File save (VFS write + Ctrl/Cmd+S)
8. ✅ Editor tabs
9. ✅ Line numbers in editor
10. ✅ Undo / Redo
11. ✅ Language support (FileType + LanguageParser EP)
12. ✅ Basic syntax highlighting (Kotlin / Java)
13. ✅ Find in file (Ctrl/Cmd+F)
14. ✅ Project View expand/collapse
15. ✅ Go to Line (Ctrl/Cmd+G)
16. ✅ Replace / Replace All (Ctrl/Cmd+R)
17. ✅ Cursor line/column in status bar
18. ✅ Confirm on close unsaved tab
19. ✅ File menu (Open Project / Save / Exit)
20. ✅ Run (Gradle + output panel)
21. ✅ Terminal
22. ✅ Find in Files (Ctrl/Cmd+Shift+F)
23. ✅ Git status in Project View
24. ✅ Git stage / commit / diff tool window
25. ✅ Git push / pull
26. ✅ Recent projects
27. ✅ Structure view
28. ✅ Breadcrumbs
29. ✅ Welcome screen
30. ✅ Settings
31. ✅ Tab indent (spaces by tab size)
32. ✅ Breakpoints (debugger foundation)
33. ✅ Debug session (JDWP launch)
34. ✅ JDI attach / Install BPs / Resume
35. ✅ Step Over / Into / Out
36. ✅ Variables view
37. ...
34. ...

## Contributing

Contributions are welcome! Please follow the coding style and open issues/PRs.

## License

Apache License 2.0 — see [LICENSE](LICENSE)
