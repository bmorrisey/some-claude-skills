# Bootstrap Checklist — New ATAK Plugin

Ordered start-to-finish steps to take a clean ATAK CIV SDK to the first
sideloadable APK. Complete in order; later steps assume earlier ones are done.

Symbols:
- 📁 = file/folder action
- 🔧 = code or config edit
- 🧪 = verify
- ⚠️ = common-failure step — read carefully

Templates referenced live in `../templates/`. Reference docs in `../references/`.

---

## Phase 1 — Project skeleton

1. 📁 **Pick a project location.** Two valid layouts (`references/01-from-scratch.md` §1):
   - **A.** Inside `<SDK>/plugins/<name>/` — quickest for local dev only.
   - **B.** Standalone repo with `atak-sdks/<version>/` committed via Git LFS — required for CI/CD. **Prefer B** unless the plugin will never leave one machine.

2. 📁 **Copy the SDK starting template:**
   `<SDK>/samples/plugintemplate/` → your new project root.

3. 🔧 **Rename the project everywhere.** Decisions to make first:
   - **Repo / project name** (kebab-case): e.g. `my-plugin`
   - **Java/Kotlin package**: e.g. `com.atakmap.android.myplugin.plugin`
   - **Main class name**: e.g. `MyPlugin`

   Then rename:
   - `settings.gradle` → set `rootProject.name = '<my-plugin>'` (template: `settings.gradle`)
   - `app/build.gradle` → set `android.namespace = '<package>'` (template: `app-build.gradle`)
   - `app/src/main/java/<old-path>/` → rename to your package path; update every `.kt`/`.java` `package` declaration
   - `app/src/main/assets/plugin.xml` → set `impl` to your fully-qualified class name (template: `plugin.xml`)
   - `app/src/main/res/values/strings.xml` → set `app_name` + `app_desc` (template: `strings.xml`)

4. ⚠️ **Use the canonical `app/build.gradle` from `templates/app-build.gradle`.** The stock SDK template's build.gradle is missing critical bits (CI-safe `compileOnly` guard, dependencyLocking, AndroidX version overrides). Replace wholesale — don't try to merge. Only change: `android.namespace` and `ext.PLUGIN_VERSION`.

5. 🔧 **Replace `app/proguard-gradle.txt`** with `templates/proguard-gradle.txt` — keeps the `<atak.proguard.mapping>` placeholder needed for TPC release builds.

6. 🔧 **AndroidManifest.xml** — use `templates/AndroidManifest.xml`. The `com.atakmap.app.component` activity is REQUIRED for plugin discovery on ATAK 4.6.0.2+.

7. 🔧 **Main plugin class** — use `templates/MyPlugin.kt` as a skeleton. Implements `gov.tak.api.plugin.IPlugin` (SDK 5.5+ style). The class name must match `impl=` in `plugin.xml`.

---

## Phase 2 — SDK jars (Layout B only — recommended)

8. 📁 **Create `atak-sdks/<version>/` at repo root.** Copy these two files from the SDK root into it:
   - `main.jar` (~33 MB — the ATAK API the plugin compiles against)
   - `atak-gradle-takdev.jar` (the Gradle plugin)

   `<version>` MUST match `ext.ATAK_VERSION` in `app/build.gradle` (default `5.7.0`).

9. ⚠️ **Set up Git LFS BEFORE adding the jars.** Once.
   ```bash
   git lfs install
   # save templates/gitattributes as .gitattributes at repo root
   git add .gitattributes
   git add atak-sdks/
   ```

10. 🔧 **Save `templates/gitignore` as `.gitignore`** at repo root. (Note the scoping: `app/**/*.keystore`, NOT `**/*.keystore` — protects `atak-sdks/`.)

---

## Phase 3 — Icons

11. 📁 **Launcher icon** — `ic_launcher.png` in all 5 mipmap density buckets (`mdpi` 48px through `xxxhdpi` 192px). Android Studio's Image Asset Studio is fastest. See `references/01-from-scratch.md` §4.1.

12. 📁 **Tool-tray icon** — `app/src/main/res/drawable/ic_tray.png`, 96×96 white-symbol-on-transparent. Required for `R.drawable.ic_tray` reference in `MyPlugin.kt`.

13. 📁 **Radial menu icons (optional)** — only if your plugin uses a radial menu. 64×64 white-on-transparent PNGs at `app/src/main/assets/icons/<name>.png`.

---

## Phase 4 — Local build & sideload

14. 🔧 **Save `templates/local.properties` as `local.properties`** at repo root, set `sdk.dir`. (DO NOT commit — covered by `.gitignore`.)

15. 🧪 **In Android Studio:**
    - Open the plugin project folder (NOT the SDK root).
    - JDK 17 (File → Project Structure).
    - Build Variants panel → select **`civDebug`**.
    - Edit `app` run config → Launch: **Nothing**.

16. 🧪 **First build:**
    ```bash
    ./gradlew assembleCivDebug
    ```
    Output APK lands at `app/build/outputs/apk/civ/debug/*.apk`.

17. 🧪 **Sideload onto a device running the matching `atak.apk`** (from the SDK root). Plugin should appear in ATAK's plugin manager and load.

If steps 16–17 fail, see "Common gotchas" in `references/01-from-scratch.md` §10.

---

## Phase 5 — CI (GitHub Actions)

18. 📁 Create `.github/workflows/` at repo root. Copy in:
    - `templates/github-workflows/android.yml`     → `.github/workflows/android.yml`
    - `templates/github-workflows/atak-versions.json` → `.github/atak-versions.json`

19. 🔧 **Edit `.github/atak-versions.json`** to list the ATAK versions you support. Start small:
    ```json
    ["5.7.0"]
    ```
    Add more versions only after confirming each compiles locally with `ATAK_VERSION=<ver> ./gradlew assembleCivDebug`.

20. 🧪 **Push to GitHub.** The `android.yml` workflow runs on push/PR to main; it builds the `civDebug` APK across all matrix versions and uploads APKs + test reports as artifacts.

---

## Phase 6 — TPC release packaging (when ready to ship)

21. 📁 Copy `templates/github-workflows/tak-tpc-release.yml` → `.github/workflows/tak-tpc-release.yml`. No edits needed — it reads versions from `.github/atak-versions.json` (same source as `android.yml`).

22. 🔧 **Publish a GitHub Release** (tag `v0.1.0` etc.). The workflow triggers on release-published events ONLY (never on push). It produces one source ZIP per ATAK version, attached as release assets.

23. 🔧 **Manual submission:** download each ZIP, upload separately at https://tak.gov/user_builds. TPC returns a signed APK + AAB per submission.

See `references/01-from-scratch.md` §8 for the full TPC flow and pre-submission verification command.

---

## Phase 7 — Unit testing (optional but recommended)

24. 🧪 The `app-build.gradle` template already includes JUnit 4 + MockK + `returnDefaultValues=true`. Run JVM unit tests with:
    ```bash
    ./gradlew testCivDebugUnitTest
    ```

25. ⚠️ **For MockK to mock your Kotlin classes**, declare them `open`:
    ```kotlin
    open class MyService(...) {
        open val someProperty: String get() = ...
    }
    ```

26. ⚠️ **For async classes**, inject the executor as a constructor parameter so tests can pass a `DirectExecutor`. See `references/01-from-scratch.md` §7.

---

## Done

The plugin now:
- Builds locally (`./gradlew assembleCivDebug`)
- Loads in a device running matching ATAK
- Builds in CI across the version matrix
- Has a working TPC release pipeline ready for `gh release create`

Next: implement the actual feature. Start small — one toolbar button opening one pane is enough for the first commit. Iterate from there.
