# Contributing to NexIDE

## Standard

See [docs/engineering-kit/NEXIDE.md](docs/engineering-kit/NEXIDE.md) and
[docs/engineering-kit/standards/REVIEW_CHECKLIST.md](docs/engineering-kit/standards/REVIEW_CHECKLIST.md).

## Build

```bash
./gradlew compileKotlin
./gradlew :platform:core:test :platform:editor:test :ide-app:test
./gradlew :ide-app:run
```

JDK 17+ (21 recommended).

## Pull requests

- One logical change per PR
- No drive-by renames or mass reformat
- New Kotlin files: Apache 2.0 header, 2-space indent
- Note any tests you could not run

## License

Apache License 2.0 — see [LICENSE](LICENSE).
