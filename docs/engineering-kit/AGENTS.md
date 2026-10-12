# NIXEND IDE — Agent Instructions

## Source of truth

1. `standards/INTELLIJ_PLATFORM_CODING_GUIDELINES_1.0.md`
2. Existing repository conventions that do not conflict with the standard.
3. Task-specific requirements.

If sources conflict, report the conflict and ask for a decision when it materially affects implementation. Do not silently override the standard.

## Project identity

This repository is an IDE analogue project. Do not apply Minecraft/Paper-specific rules to it.

## Required workflow

- Inspect before editing.
- Keep one logical change per patch/PR.
- New files: Kotlin by default; modify existing Java where necessary.
- Add tests for new features and bug fixes.
- Run relevant tests and available inspections.
- Review the full diff before finishing.
- State exactly which commands ran and their observed result.
- Do not claim validation that was not performed.

## Standard highlights

- Kotlin priority; idiomatic Kotlin and null-safety.
- Prefer immutable objects and interfaces.
- 2-space indentation; max line length 140.
- `else`, `catch`, `finally` on a new line.
- `my` instance prefix; `our` static/class prefix.
- Apache 2.0 copyright header and contributor name in each new file.
- `.editorconfig` and shared project code style.
- Reformat Code (Ctrl+Alt+L) before commit.
- No warnings from the shared Inspection Profile.
- Functional tests should cover most functionality; add tests for features and bug fixes.
- New OpenAPI requires Javadoc/KDoc.
- In `catch (Throwable)` or `catch (Exception)`, call `rethrowControlFlowException` first.
- Never catch `CancellationException` manually.
- Preserve backward compatibility; avoid unnecessary rename/move.
- Do not mix functional changes, formatting-only changes, and warning cleanup in one patch.
- Do not include `.idea/*` or `codeStyleSettings.xml` changes unless required by the fix.
- Before push, remove unfinished code and TODOs.
- Build and test against the latest `intellij-community` when feasible.
- For Kotlin build scripts: use `@JvmField` for internal data class fields; prefer `.put()` / `.get()` over `[]`; prefer explicit `HashMap` / `HashSet` / `LinkedHashMap` over `mutableMapOf()` / `mutableSetOf()`.

## Error handling example

```kotlin
try {
  doWork()
}
catch (e: Throwable) {
  rethrowControlFlowException(e)
  LOG.error(e)
}
```

Use the actual imports and logging API present in the repository. Do not copy the example blindly if the project API differs; verify it first.
