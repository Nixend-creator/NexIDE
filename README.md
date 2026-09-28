# NexIDE

**Modern IDE inspired by IntelliJ IDEA**

NexIDE is a full-featured desktop IDE built with Kotlin and Compose Multiplatform.
It aims to provide a powerful editing experience with PSI, plugin system, debugger and more.

## Status

🚧 Early development (v0.1.0-SNAPSHOT)

Current capabilities:
- Project skeleton & multi-module architecture
- Basic Application / Project model
- Compose Desktop UI bootstrap
- Virtual File System (LocalFileSystem + VirtualFile)
- Basic Editor (Document + Editor + Compose EditorView)
- PSI foundation (PsiElement, PsiFile, PsiManager, plain-text fallback)
- Plugin system (Extension Points + PluginManager)
- Project View (file tree based on VFS)
- File save from Editor (Ctrl/Cmd+S + button)
- Editor tabs (multiple open files)
- Line numbers in editor
- Undo / Redo (Ctrl+Z / Ctrl+Y)
- Language support (FileType EP + LanguageParser EP + PSI)
- Basic syntax highlighting (Kotlin / Java keywords, strings, comments)
- Find in file (Ctrl/Cmd+F)
- Project View expand/collapse

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
15. Debugger integration
16. ...

## Contributing

Contributions are welcome! Please follow the coding style and open issues/PRs.

## License

Apache License 2.0 — see [LICENSE](LICENSE)
