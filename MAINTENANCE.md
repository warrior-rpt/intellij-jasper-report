# MAINTENANCE.md

How this plugin is kept current as IntelliJ, Gradle, Java, and the IntelliJ Platform Gradle Plugin release new versions. Written so a human **or an agent** can follow it end to end.

**Principle: automation proposes, the owner approves.** Agents prepare branches and PRs. They never merge to `main`, never publish to the Marketplace.

---

## 1. Baseline (update this table whenever it changes)

| Component | Where it is set | Value (as of 2026-11-13) |
|---|---|---|
| Target IDE (`ideaVersion`) | `gradle.properties` | `2025.3.3` |
| Plugin version (`version`) | `gradle.properties` **and** `plugin.xml` `<version>` | `1.0.4` |
| `sinceBuild` | `build.gradle` → `pluginConfiguration.ideaVersion` | `253` (= 2025.3) |
| `untilBuild` | same | `null` (open-ended) |
| IntelliJ Platform Gradle Plugin | `build.gradle` `plugins {}` | `2.13.1` |
| Gradle wrapper | `gradle/wrapper/gradle-wrapper.properties` | `9.0` |
| Java toolchain | `build.gradle` `java.toolchain` | `21` |
| JUnit | `build.gradle` `testImplementation` | `4.13.2` |
| Plugin Verifier targets | `build.gradle` `pluginVerification.ides` | `recommended()` |

This table is a snapshot, not a source of truth. If it disagrees with the files, the files win; fix the table.

---

## 2. The three layers

### Layer A: Continuous detection (cheap, no approval needed)
Detects new versions and opens PRs. It changes nothing on `main`.

| Concern | Tool | Status |
|---|---|---|
| Gradle plugins, libraries, Gradle wrapper | GitHub Dependabot (`gradle` ecosystem) via `.github/dependabot.yml`. Monthly schedule. (Confirm on its first run that it also proposes wrapper bumps; if not, handle the wrapper in the monthly agent run.) | **Config added** (`.github/dependabot.yml`); active once pushed to `main` |
| GitHub Actions versions (once workflows exist) | Dependabot (`github-actions` ecosystem) | **Config added**, same file |
| New IntelliJ IDEA release (`ideaVersion`) | Dependabot cannot see this. Use a scheduled workflow, or the monthly agent run (section 3), that compares `gradle.properties` to the latest release from JetBrains' releases feed | **Not set up yet** |
| Java LTS | Checked by the monthly agent run. Rule: follow the Java version the *target IDE* is built on (JBR). Do not jump ahead of it. | Manual/agent |

### Layer B: CI gate on every PR
A GitHub Actions workflow (`.github/workflows/ci.yml`) running on PRs and pushes to `main`:
`./gradlew build verifyPluginStructure verifyPlugin`. It also runs monthly on a cron and on manual dispatch, because `recommended()` resolves to the newest IDEs and so catches breakage from new IntelliJ releases even with no code change. It uploads the built plugin zip as a build artifact. **Status: workflow file added, not yet run on GitHub** (first run happens after the branch is pushed; fix any CI-only problems then). Until it has passed once, also run the gates locally (section 4.3) and paste the results into the PR.

### Layer C: Monthly maintenance run (agent + owner approval)
A scheduled agent run, **monthly, plus ad hoc when JetBrains ships a major IDE release** (they ship roughly three a year: `.1`, `.2`, `.3`). It follows section 4 of this file, opens **one PR**, and stops. The owner reviews, merges, and uploads manually.

How to schedule it (pick one; all keep the owner as approver):
- **Claude Code scheduled/cloud routine** (the `/schedule` skill): monthly cron, prompt = *"Follow MAINTENANCE.md section 4 in this repo. Open a PR. Do not merge or publish."*
- **Claude Code local scheduled task** on the owner's Mac: same prompt, runs only while the machine is on.
- **Manual**: owner says "run the monthly maintenance" in a Claude Code session in this repo.

The agent's only outputs are a branch, a PR, and a new row in the log (section 7).

**Status: scheduled** as a Claude Code cloud routine, "Jasper plugin monthly maintenance" (id `trig_01TVc67Ti4xQtMjVnuBgFTLp`, manage/pause at <https://claude.ai/code/routines/trig_01TVc67Ti4xQtMjVnuBgFTLp>). Cron `0 10 15 * *` (UTC): the 15th of each month, 2am Vancouver in winter (3am in summer). Model `claude-sonnet-5-5`, no connectors, repo `warrior-rpt/intellij-jasper-report`. The routine's prompt tells the agent to read `AGENTS.md` and `MAINTENANCE.md` and follow section 4, so **changes to this runbook change what the routine does**. If the routine is ever recreated, copy its prompt from the routine page and update the id here.

---

## 3. Policies (decisions the agent must follow, not make)

**Versioning.** Semantic-ish: maintenance-only changes (IDE/Gradle/dependency bumps, no behavior change) → patch bump (`1.0.4` → `1.0.5`). New user-visible feature → minor. Bump in **both** `gradle.properties` and `plugin.xml`, and add a `<change-notes>` entry (newest first). Skip the version bump if nothing user-affecting changed and the owner doesn't want a release; ask in the PR.

**`sinceBuild` (the one that can hurt users).** It decides the oldest IDE that can install the plugin. It is currently `253`, meaning users on IDE 2025.2 or older cannot install it. **Never raise it on your own.** Raise only if a required API doesn't exist in older builds, and then say so loudly in the PR. *Open question for the owner:* whether to lower `sinceBuild` to reach more users. That needs a check that the code compiles and the verifier passes against the older platform.

**`untilBuild`.** Keep `null` (open-ended). The Plugin Verifier is what protects against future breakage.

**Target IDE.** Track the latest stable (non-EAP) release. Never target EAP/RC builds in a release PR.

**Java.** Match the JBR/Java level the target IDE requires. Don't use language features newer than the lowest supported IDE's Java level.

**Deprecated/removed API.** If an upgrade needs code changes beyond trivial renames, do the minimal fix, explain it in the PR, and keep behavior identical.

---

## 4. Monthly runbook (the agent follows this)

Work in `/Users/Rithin/IdeaProjects/intellij-jasper-report` (or a fresh clone). All commands from the repo root.

### 4.0 Pre-flight
```bash
git checkout main && git pull --ff-only
git status --short            # expect only .intellijPlatform/self-update.lock (known noise). Otherwise STOP and ask.
./gradlew build               # baseline must be green BEFORE any change. If red, STOP and report.
```
Create a branch: `git checkout -b maintenance/YYYY-MM`.

### 4.1 Find what's new
Check each row of the section 1 table against the latest stable release:
- **IntelliJ IDEA**: latest stable version from JetBrains releases (<https://www.jetbrains.com/idea/download/other.html> or the releases feed). 
- **IntelliJ Platform Gradle Plugin**: latest release (Gradle Plugin Portal: `org.jetbrains.intellij.platform`; docs at <https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html>).
- **Gradle**: <https://gradle.org/releases/>. Check the IntelliJ Platform Gradle Plugin's docs for the minimum/maximum Gradle it supports before bumping.
- **Java**: which JBR/Java version the newest IDE ships with.
- **JUnit 4.13.2** is the final 4.x release; no action unless moving to JUnit 5 (not planned).

If nothing is newer: record "no updates" in the log (section 7), open no PR, stop.

### 4.2 Apply updates, **one logical change per commit**
Order matters; each step should leave the build green:
1. **Gradle wrapper**: `./gradlew wrapper --gradle-version <X>` (run twice so the wrapper scripts refresh).
2. **IntelliJ Platform Gradle Plugin**: edit the `version` in `build.gradle` `plugins {}`.
3. **Target IDE**: edit `ideaVersion` in `gradle.properties`.
4. **Java toolchain**: edit `languageVersion` in `build.gradle` only if step 4.1 says the IDE requires it.
5. **Version + change-notes**: per section 3.

After each step: `./gradlew compileJava`. After all steps, the gates below.

### 4.3 Gates (all must pass)
```bash
./gradlew clean build           # compile + 4 tests + assemble
./gradlew verifyPluginStructure
./gradlew verifyPlugin          # Plugin Verifier vs recommended IDEs; slow, downloads IDEs, needs network
./gradlew buildPlugin           # confirm build/distributions/intellij-jasper-report-<version>.zip exists
```
Read the verifier output, not just the exit code: **compatibility problems and deprecated/scheduled-for-removal API usages** must be zero or explicitly explained in the PR.

### 4.4 Open the PR (do not merge)
- Title: `Maintenance YYYY-MM: <what changed>`.
- Body: a table of old → new for every component, the gate results (pass/fail + notable verifier warnings), any code changes and why, whether `sinceBuild` changed (it should not), and a **"Release checklist for owner"** (section 5).
- **No AI attribution lines** (see `AGENTS.md` rule 3).
- Stage files by name; never `git add -A` (it would sweep up `.intellijPlatform/self-update.lock`).

### 4.5 Stop conditions (stop, report, do not push through)
- Baseline build red before changes.
- Any gate fails after a straightforward fix attempt.
- The upgrade needs `sinceBuild` raised, a behavior change, or removal of a feature.
- Anything unexpected in `git status`.
In these cases open the PR as **draft** (or just report in chat) with exactly what failed.

---

## 5. Release checklist (owner, manual)

After merging the maintenance PR:
1. `git checkout main && git pull`, then `./gradlew clean buildPlugin`.
2. Take `build/distributions/intellij-jasper-report-<version>.zip`.
3. Upload at <https://plugins.jetbrains.com/> (plugin "Jasper Report", id `jasper-report`) → *Upload update*. Marketplace runs its own compatibility checks and review.
4. Tag the release on GitHub: `git tag v<version> && git push origin v<version>` (optional but recommended).

Automating step 3 (`publishPlugin` + a Marketplace token in GitHub secrets) is possible but deliberately **not** done: the owner wants to approve every upload. Revisit only if the owner asks.

---

## 6. Known quirks

- **`.intellijPlatform/self-update.lock` is tracked by git** even though `.gitignore` lists `.intellijPlatform/`, so it shows as modified after every Gradle run. Never commit it. One-time cleanup for the owner: `git rm -r --cached .intellijPlatform && git commit -m "Stop tracking .intellijPlatform"`.
- **`buildPlugin` log noise**: stack traces and `WARN` lines from `buildSearchableOptions` are normal. Check the exit code.
- **Remote URL moved**: `origin` is `https://github.com/rithin123/intellij-jasper-report.git`, which redirects to `https://github.com/warrior-rpt/intellij-jasper-report.git`. Fix: `git remote set-url origin https://github.com/warrior-rpt/intellij-jasper-report.git`. Update any Dependabot/CI config to the new name.
- **`README.md` links to `CONTRIBUTING.md`, which doesn't exist.** Either add one or remove the links.
- **`src/main/resources/intellij/jasper/report/icons/jasper*.png` are not referenced anywhere** (only `jrxml.png` is used). Likely leftover; confirm before deleting.
- Tests boot a headless IDE test fixture and print a harmless `[warning][cds]` JVM line.

---

## 7. Maintenance log (agent appends a row each run, newest at the bottom)

| Date | Run by | Result | PR / notes |
|---|---|---|---|
| 2026-11-13 | Claude Code (manual) | Created this runbook, `dependabot.yml`, and `ci.yml`. No version upgrades done in this pass | Last release before this: 1.0.4 (Jrxml language registration fix + schema-provider NPE hardening) |
| 2026-11-13 | Claude Code (manual) | Scheduled the monthly cloud routine (section 2, Layer C) and triggered a test run | See the routine page for the test run's result |
