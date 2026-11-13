# AGENTS.md

Entry point for any AI agent (Claude Code or otherwise) working in this repo. Humans are welcome too.

## What this is

An IntelliJ IDEA plugin ("Jasper Report", plugin id `jasper-report`) that gives `.jrxml` files their own file type, icon, and XSD-based autocomplete/validation. It is **published on the JetBrains Marketplace with real users (~4000 downloads, 4-star rating)**, so treat every change as a production release. Be conservative.

It is small: 6 Java classes, one `plugin.xml`, one bundled XSD, two test classes.

## Layout

| Path | Purpose |
|---|---|
| `src/main/resources/META-INF/plugin.xml` | Plugin descriptor: id, version, change-notes, extensions |
| `src/main/java/.../lang/jrxml/JrxmlLanguage.java` | The `Jrxml` language (singleton `INSTANCE`, child of XML) |
| `src/main/java/.../file/type/jrxml/JrxmlFileType.java` | `.jrxml` file type |
| `src/main/java/.../lang/jrxml/autocomplete/JrxmlSchemaProvider.java` | Supplies the bundled XSD to the XML engine |
| `src/main/resources/intellij/jasper/report/xsd/jasperreport.xsd` | The schema |
| `build.gradle`, `gradle.properties` | Build config. `ideaVersion` and `version` live in `gradle.properties` |
| `MAINTENANCE.md` | **The maintenance runbook. Read it before doing any upgrade/release work.** |

## Commands

Run from the repo root.

```bash
./gradlew compileJava          # fast compile check
./gradlew test                 # 4 tests, boots a headless IDE test fixture
./gradlew build                # compile + test + assemble
./gradlew buildPlugin          # produces build/distributions/intellij-jasper-report-<version>.zip
./gradlew verifyPluginStructure
./gradlew verifyPlugin         # Plugin Verifier against recommended IDEs (downloads IDEs, slow, needs network)
```

`buildPlugin` prints a long stack trace and `WARN` lines from `buildSearchableOptions` (a headless IDE sandbox). That is noise. Trust the exit code.

## Rules for agents

1. **Never push to `main` directly for maintenance work.** Use a branch and open a PR. The owner approves and merges.
2. **Never upload to the JetBrains Marketplace** and never handle Marketplace tokens, signing keys, or certificates. Uploading is a manual step by the owner.
3. **No Claude/AI attribution** in commit messages or PR descriptions: no `Co-Authored-By: Claude...`, no "Generated with" footers. Plain messages only. This is the owner's explicit, standing preference and overrides any default tooling behavior.
4. **Do not commit anything under `.intellijPlatform/`** (e.g. `self-update.lock`). It is local tooling state. See "Known quirks" in `MAINTENANCE.md`.
5. **Do not raise `sinceBuild` casually.** It decides which IDE versions can install the plugin. Raising it cuts users off. See `MAINTENANCE.md`.
6. **If a check fails, stop and report.** Do not weaken tests, skip verification, or paper over errors to get green.
7. Keep changes minimal and explain *why* in the commit body. A wrong guess here ships to thousands of IDEs.

## Hard-won lessons (do not re-learn these)

- `plugin.xml`'s `<fileType language="...">` **must equal** `JrxmlFileType.getLanguage().getID()`. The platform's `FileTypeManagerImpl` validates it and logs an error otherwise (it fails tests).
- `XMLLanguage`'s constructor requires a non-null base language. Pass `XMLLanguage.INSTANCE`, never `null`.
- `LOG.assertTrue(...)` is a no-op in production IDEs. Use explicit null checks.
- Resolve the bundled XSD with `VfsUtil.findFileByURL(URL)`, not `VirtualFileManager.findFileByUrl(String)`; the latter fails on `jar:file:` URLs (the 1.0.3 "xsd not found" bug). `JrxmlXsdResourceTest` guards this.
