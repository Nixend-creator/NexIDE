# Code Review Report — NexIDE post shell/CI (2026-10-12)

## Рішення
- [x] Відповідає з зауваженнями

## Scope reviewed
Recent architecture and process work: `IdeShellActions`, exit confirm, `NexIdeVersion`,
CI workflow, Window integrity fix, engineering kit import.

Automated scan: brace balance, `IdeMenuBar` presence, Apache headers on ide-app sources,
tab characters, expected test/CI files.

## Blocker
- Немає.

## Major
- **`Main.kt` still large (~1250 LOC).** Wiring is improved via `IdeShellCallbacks`, but
  composition root remains a regression magnet. Prefer further extraction only when
  touching related areas (do not mass-move).
- **Gradle/test not executed in this review environment** (historical sandbox timeouts /
  display limits). CI is defined but not proven green on a real GitHub runner from here.

## Minor
- **`my`/`our` prefixes** not applied consistently across older Compose UI code (pre-existing;
  do not bulk-rename).
- **Some lines in `Main` may exceed 140 characters** (lambda wiring). Acceptable for
  composition root; avoid growing further.
- **README CI badge** still uses placeholder `OWNER` — replace when publishing the repo.
- **intellij-community build item** on generic checklist is N/A; use `NEXIDE.md` commands.

## Checklist (condensed)

| Item | Status |
|------|--------|
| One logical theme per recent commits (shell / CI / about) | OK (separate commits) |
| No accidental rename/move | OK |
| No unfinished TODO from these tasks | OK |
| New files Kotlin + Apache header | OK |
| 2-space indent / no tabs (sampled) | OK |
| Invented IntelliJ Platform API | N/A (custom platform) |
| Tests for IdeModuleActions / IdeWindowTitle | Present |
| CI workflow present | `.github/workflows/ci.yml` |
| Run tests / compile in this environment | **Not run** |

## Перевірки
| Перевірка | Команда/спосіб | Результат |
|---|---|---|
| Тести | `./gradlew :platform:core:test :platform:editor:test :ide-app:test` | Не виконано тут |
| Build | `./gradlew compileKotlin` | Не виконано тут |
| Integrity scan | brace / MenuBar / headers | OK |
| Форматування | manual sample | OK enough |

## Невиконані перевірки та причини
- Full Gradle compile/test: sandbox historically unstable for long Gradle runs; agent must
  not claim green without output.
- Shared Inspection Profile / IDE Reformat: no IntelliJ IDE in agent environment.

## Рекомендації
1. On developer machine: run CI-equivalent Gradle commands once and fix any compile errors.
2. Next structural change: only if needed — extract keymap wiring, not wholesale Main rewrite.
3. Keep engineering kit under `docs/engineering-kit/`; agents start from `NEXIDE.md`.

## Змінені файли (kit import + adaptation)
- `docs/engineering-kit/**` (full kit)
- `docs/engineering-kit/NEXIDE.md` (project mapping)
- `docs/engineering-kit/GROK.md` (NexIDE pointer)
- `README.md` (Engineering kit section)
- `docs/engineering-kit/reports/2026-10-12-shell-ci-audit.md` (this report)
