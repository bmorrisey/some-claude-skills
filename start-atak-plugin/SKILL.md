---
name: start-atak-plugin
description: Bootstrap a brand-new ATAK CIV SDK plugin from scratch — project skeleton, build.gradle, plugin.xml, AndroidManifest.xml, CI workflows, TPC release packaging. Use when the user wants to create a new ATAK plugin, start an ATAK plugin project, build an ATAK plugin from scratch, scaffold a new ATAK plugin, or set up a new plugin in the ATAK CIV SDK. Targets SDK 5.5+ (IPlugin style). Includes proven templates and CI configs lifted from a shipped, production plugin.
---

# Start a New ATAK Plugin

This skill bootstraps a working ATAK plugin from an empty directory to a first
sideloadable APK with CI and TPC release packaging in place. Every gotcha
documented here was discovered the hard way during real plugin development; the
templates encode the fixes.

**Audience:** You are an AI coding agent with access to an unzipped ATAK CIV SDK
(typically at a path like `D:\Dev\ATAK-CIV-5.7.0.5-SDK\` or
`~/ATAK-CIV-5.7.0.5-SDK/`). The user has asked for a new plugin.

---

## When to use this skill

Invoke when the user says any of:
- "Start a new ATAK plugin"
- "Bootstrap / scaffold an ATAK plugin"
- "Create an ATAK plugin from scratch"
- "Set up the ATAK SDK template for X"
- "I want to build a plugin for ATAK"

If the user already has an existing plugin project and wants to add a feature,
do NOT use this skill — work in their existing repo using its conventions.

---

## What this skill contains

```
start-atak-plugin/
├── SKILL.md                              ← this file (you are here)
├── checklists/
│   └── bootstrap-checklist.md            ← ORDERED 26-step workflow — work through this
├── references/
│   ├── 01-from-scratch.md                ← deep dive: every gotcha + why (READ FOR CONTEXT)
│   ├── 02-sdk-quick-reference.md         ← SDK file structure + architecture overview
│   └── 03-sdk-index.md                   ← every file in the SDK, by directory
└── templates/                            ← paste-ready files; placeholders marked MYPLUGIN
    ├── app-build.gradle                  ← the heavily-modified one — DO NOT use the SDK default
    ├── settings.gradle
    ├── AndroidManifest.xml
    ├── plugin.xml
    ├── MyPlugin.kt                       ← minimal IPlugin skeleton
    ├── proguard-gradle.txt               ← contains <atak.proguard.mapping> placeholder required for TPC
    ├── strings.xml                       ← app_name + app_desc
    ├── local.properties                  ← per-developer; not committed
    ├── gitattributes                     ← save as .gitattributes — Git LFS for SDK jars
    ├── gitignore                         ← save as .gitignore — scopes keystore ignore correctly
    └── github-workflows/
        ├── android.yml                   ← matrix CI build → debug APKs
        ├── tak-tpc-release.yml           ← on release published → source ZIPs per ATAK version
        └── atak-versions.json            ← single source of truth for supported ATAK versions
```

---

## Workflow

1. **Read `checklists/bootstrap-checklist.md` end to end first.** It's the
   ordered 26-step plan. Do not skip ahead — the steps build on each other.

2. **Skim `references/01-from-scratch.md` §10 (Common Gotchas Summary).** Five
   minutes there saves an hour of debugging "all com.atakmap.* imports fail in
   CI" later.

3. **Ask the user three questions BEFORE touching files** (use AskUserQuestion):
   - Plugin name? (kebab-case repo name, e.g. `my-plugin` — becomes APK name)
   - Java/Kotlin package? (e.g. `com.atakmap.android.myplugin.plugin`)
   - Project layout? Recommend **Layout B** (standalone repo, atak-sdks/ in LFS)
     unless they explicitly want SDK-internal placement.

4. **Bootstrap by working through the checklist phases:**
   - Phase 1: Project skeleton (copy `samples/plugintemplate/`, rename everything)
   - Phase 2: SDK jars + Git LFS
   - Phase 3: Icons (delegate to user — they need to provide artwork)
   - Phase 4: Local build + sideload
   - Phase 5: CI (GitHub Actions)
   - Phase 6: TPC release packaging
   - Phase 7: Unit tests

5. **Use the templates verbatim where possible.** Only edit the marked
   `MYPLUGIN` placeholders. The templates encode hard-won fixes; ad-hoc edits
   reintroduce bugs.

---

## Top three failure modes to prevent

### 1. "All `com.atakmap.*` imports fail in CI"
**Cause:** takdev plugin's flatDir fallback looks for `../../main.jar`. In the SDK
layout that file exists at the SDK root. In CI the checkout IS the root, so
nothing's two levels up — every ATAK import unresolved.
**Fix:** Use `templates/app-build.gradle`. It contains the critical block:
```groovy
if (!isDevKitEnabled()) {
    compileOnly       files("${rootDir}/atak-sdks/${ATAK_VERSION}/main.jar")
    testImplementation files("${rootDir}/atak-sdks/${ATAK_VERSION}/main.jar")
}
```
The `if (!isDevKitEnabled())` gate is mandatory — without it, TPC's pipeline
fails (their source ZIP excludes `atak-sdks/`).

### 2. "`validateSigningCivDebug FAILED` in CI"
**Cause:** takdev plugin generates `android_keystore` at configuration time, but
only when the devkit repo resolves. CI prints "Skipping civDebug" and never
creates the keystore.
**Fix:** `templates/github-workflows/android.yml` already has the `keytool`
step before `assembleCivDebug`. Don't remove it.

### 3. "Plugin built fine but won't load in ATAK"
**Causes & checks:**
- Wrong build variant — must be `civDebug`, not `debug` or `mainDebug`.
- ATAK on device is a Play Store build, not the SDK-bundled `atak.apk`. Plugins
  signed with the dev keystore won't load on Play Store ATAK.
- `ATAK_VERSION` in build.gradle doesn't match the ATAK version installed.
  Check `VERSION.txt` in the SDK root.
- `com.atakmap.app.component` activity missing from `AndroidManifest.xml`
  (required for ATAK 4.6.0.2+ discovery).

---

## Hard requirements (do not negotiate)

These are locked by the ATAK SDK / takdev plugin. Changing any of them breaks
the build in non-obvious ways:

| Tool | Version |
|---|---|
| AGP (Android Gradle Plugin) | **8.9.0** |
| Gradle | **8.13** |
| JDK | **17** |
| Build variant for sideload | **civDebug** |

Plugin XML must declare the new-style IPlugin extension type
(`gov.tak.api.plugin.IPlugin`). Legacy `MapComponent` / `DropDownReceiver` still
works on SDK 5.5+ but new plugins should use IPlugin — see
`references/02-sdk-quick-reference.md` §3.

---

## Decision: project layout (ask the user)

Two valid layouts. **Recommend B unless the user has a specific reason for A.**

| | A. Inside SDK | B. Standalone repo (recommended) |
|---|---|---|
| Layout | `<SDK>/plugins/<name>/` | `<name>/` + `atak-sdks/<ver>/` in Git LFS |
| Local dev | Works | Works |
| CI/CD (GitHub Actions, etc.) | **Broken** — checkout has no SDK | **Works** — SDK in repo |
| Repo size | Tiny | +33 MB per SDK version (LFS-tracked, doesn't bloat history) |
| Multi-version testing | Awkward | Trivial — add to `.github/atak-versions.json` |

If A is chosen, the CI templates are unusable. Layout B is the path for any
plugin meant to outlive a single developer's machine.

---

## After the bootstrap

The bootstrap leaves the plugin in a working but minimal state — one toolbar
button, opens nothing. The next job is the actual feature work, which is
entirely up to the user's requirements (it could be telemetry, image overlays,
form input, networking — anything).

For SDK API patterns, the canonical references are:

| Need | Where to look |
|---|---|
| API surface | `<SDK>/atak-javadoc.jar` — link into Android Studio for hover docs |
| Lifecycle, DropDownReceiver, map interaction | `<SDK>/ATAK_Plugin_Development_Guide.pdf` |
| Working code examples | `<SDK>/samples/` — 25+ self-contained sample plugins (KML, video, sensor, BT, radial menu demo, etc.) |
| Which libraries ATAK already provides | `<SDK>/docs/dependencies.txt` (avoids Duplicate Class errors) |

The `<SDK>/samples/` directory is especially valuable — each sample is a
narrowly-scoped working plugin demonstrating one feature. Browse the sample
list in `references/03-sdk-index.md` to find one close to the target feature
before writing new code.

---

## Hand-off back to the user

After Phase 4 completes (first sideloadable APK builds), pause and let the
user verify on their device. Do not proceed to CI (Phase 5) without that
confirmation — if Phase 4 doesn't work, no amount of CI will fix it.

If the user wants to skip CI/TPC entirely (e.g., personal/throwaway plugin),
mark Phases 5–6 as deferred and proceed straight to feature implementation.
