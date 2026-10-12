# Publish NexIDE to GitHub (phone-friendly)

Repo is ready on branch `main` (Apache 2.0, CI workflow, no secrets).

## 1. Create empty repo on GitHub

1. Open [github.com/new](https://github.com/new)
2. Name: `NexIDE` (or similar)
3. **Public** or Private
4. **Do not** add README / LICENSE / .gitignore (already in project)
5. Create repository

## 2. Push from a machine that has this clone

If the code is only in this environment, copy/sync it first, then:

```bash
cd /path/to/NexIDE
git remote add origin https://github.com/<YOUR_USER>/NexIDE.git
# or SSH:
# git remote add origin git@github.com:<YOUR_USER>/NexIDE.git

git branch -M main
git push -u origin main
```

From **GitHub mobile**: after `origin` exists and you can run `git push` once (PC, Codespace, or CI upload), later pushes can use other tools.

## 3. Check CI

GitHub → **Actions** → workflow **CI**  
Should run: `compileKotlin` + unit tests (JDK 21).

## 4. Optional badge in README

```markdown
![CI](https://github.com/<YOUR_USER>/NexIDE/actions/workflows/ci.yml/badge.svg)
```

## 5. Topics (GitHub About)

Suggested: `ide`, `kotlin`, `compose-multiplatform`, `gradle`, `intellij-inspired`

## Do not commit

- `local.properties`, `~/.nexide/`, machine `JAVA_HOME` in `gradle.properties`
- `.idea/`, `build/`, `.gradle/`
