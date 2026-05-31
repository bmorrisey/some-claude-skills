# Building an ATAK Plugin from Scratch — Practical Guide

> **Audience:** An AI agent (or developer) starting a new plugin from a clean ATAK CIV SDK.
> **Companion docs:** `ATAK_plugin_reference.md` (SDK structure, architecture overview), `SDK_INDEX.md` (file listing).
> This guide covers the *practical* steps and hard-won gotchas that the SDK PDF does not.

---

## 1. Project Structure — Two Valid Approaches

### A. Inside the SDK directory (traditional, SDK default)
```
D:\Dev\ATAK-CIV-5.7.0.5-SDK\
  main.jar
  atak-gradle-takdev.jar
  samples\plugintemplate\
  plugins\
    my-plugin\          ← your project lives here
      app\build.gradle  ← references ../../main.jar and ../../atak-gradle-takdev.jar
```
The sample `build.gradle` files default to `${rootDir}/../../` paths for SDK jars. This works locally but **breaks in CI** because the repo checkout is the project root, not a subfolder of the SDK.

### B. In-repo SDK jars (recommended for CI/CD)
Commit the SDK jars inside the plugin repo under `atak-sdks/<version>/`:
```
my-plugin/
  atak-sdks/
    5.7.0/
      atak-gradle-takdev.jar   ← tracked via Git LFS
      main.jar                 ← tracked via Git LFS (~33 MB)
  app/build.gradle             ← references ${rootDir}/atak-sdks/${ATAK_VERSION}/...
```
Set up Git LFS before adding the jars:
```bash
git lfs install
# add to .gitattributes:
# atak-sdks/**/*.jar filter=lfs diff=lfs merge=lfs -text
git add .gitattributes
git lfs track "atak-sdks/**/*.jar"
git add atak-sdks/
```
Update `.gitignore` so `*.keystore` / `*.jks` excludes only `app/**` (not `atak-sdks/`).

**Why this matters:** Without in-repo jars, GitHub Actions has no SDK files and all `com.atakmap.*` imports fail to compile.

---

## 2. Renaming the Plugin Template

When starting from `samples/plugintemplate`, rename these in order:

### 2.1 Package and namespace
In `app/build.gradle`:
```groovy
android {
    namespace 'com.atakmap.android.myplugin.plugin'
    ...
}
```
In `settings.gradle` — set the project name:
```groovy
rootProject.name = 'my-plugin'
```

### 2.2 Source package directories
Rename the Java/Kotlin source directory from:
`app/src/main/java/com/atakmap/android/plugintemplate/`
to your package path, then update the `package` declarations in every `.kt` / `.java` file.

### 2.3 Main plugin class
The main class must implement `gov.tak.api.plugin.IPlugin` (new-style SDK 5.5+):
```kotlin
package com.atakmap.android.myplugin.plugin

class MyPlugin(context: IPluginContext) : AbstractPlugin(context) {
    override fun onStart() { ... }
    override fun onStop()  { ... }
}
```

### 2.4 plugin.xml
`app/src/main/assets/plugin.xml` registers the plugin with ATAK. Update the class reference:
```xml
<plugin  class="com.atakmap.android.myplugin.plugin.MyPlugin"  .../>
```

### 2.5 AndroidManifest.xml
Must include this activity for ATAK discoverability (TPC requirement):
```xml
<activity android:name="com.atakmap.app.component" tools:ignore="MissingClass">
  <intent-filter android:label="@string/app_name">
    <action android:name="com.atakmap.app.component" />
  </intent-filter>
</activity>
```

### 2.6 ProGuard repackage rule
`app/proguard-gradle-repackage.txt` is auto-generated at build time from:
```groovy
// in app/build.gradle afterEvaluate block:
project.file('proguard-gradle-repackage.txt').text =
    "-repackageclasses atakplugin.${rootProject.getName()}"
```
No manual change needed — just ensure `rootProject.name` in `settings.gradle` is correct.

---

## 3. build.gradle — Key Configuration

### 3.1 ATAK_VERSION env var (supports multi-version CI matrix)
```groovy
buildscript {
    ext.PLUGIN_VERSION = "0.1"
    ext.ATAK_VERSION = System.getenv('ATAK_VERSION') ?: '5.7.0'

    ext.takdevPlugin = getProperty('takdev.plugin',
        "${rootDir}/atak-sdks/${ATAK_VERSION}/atak-gradle-takdev.jar")
    ...
}
```

### 3.2 Dependencies — critical additions
```groovy
dependencies {
    implementation 'androidx.core:core-ktx:1.18.0'
    implementation fileTree(dir: 'libs', include: '*.jar')

    if (!isDevKitEnabled()) {
        // When no TAK maven repo is configured (local dev, our own CI), put the
        // in-repo SDK jar on the Kotlin compile classpath. When devkit IS enabled
        // (e.g. TPC's pipeline), the ATAK API resolves from their maven repo and
        // atak-sdks/ is excluded from the source ZIP — do not reference it.
        compileOnly files("${rootDir}/atak-sdks/${ATAK_VERSION}/main.jar")
        testImplementation files("${rootDir}/atak-sdks/${ATAK_VERSION}/main.jar")
    }

    testImplementation 'junit:junit:4.13.2'
    testImplementation 'io.mockk:mockk:1.13.10'
    testImplementation 'org.jetbrains.kotlin:kotlin-test-junit:2.0.21'
}
```

### 3.3 Test options (required for JVM unit tests)
```groovy
testOptions {
    unitTests {
        returnDefaultValues = true   // Android stubs return defaults, not exceptions
    }
}
```

### 3.4 Signing config
ATAK debug builds use a generated keystore at `${buildDir}/android_keystore`:
```groovy
signingConfigs {
    debug {
        storeFile file("${buildDir}/android_keystore")
        storePassword "tnttnt"
        keyAlias "wintec_mapping"
        keyPassword "tnttnt"
    }
}
```
The takdev plugin generates this keystore at configuration time when the devkit
repo resolves. **In CI it is never generated** — add a `keytool` step (see §5).

### 3.5 Versions that are hard requirements (ATAK)
```
AGP:    8.9.0
Gradle: 8.13   ← do NOT change; ATAK build system requires this
JDK:    17
```
These are locked by the ATAK SDK/takdev plugin and must not be upgraded arbitrarily.

---

## 4. Icons

The ATAK SDK has no built-in icon generation. You simply provide PNG files in
specific directories. All three icon types below need to be present before the
build will produce a shippable APK.

### 4.1 Launcher icon (`ic_launcher.png`)
Standard Android adaptive icon. Provide one PNG per density bucket:

| Density | Size (px) | Path |
|---------|-----------|------|
| mdpi    | 48 × 48   | `app/src/main/res/mipmap-mdpi/ic_launcher.png` |
| hdpi    | 72 × 72   | `app/src/main/res/mipmap-hdpi/ic_launcher.png` |
| xhdpi   | 96 × 96   | `app/src/main/res/mipmap-xhdpi/ic_launcher.png` |
| xxhdpi  | 144 × 144 | `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` |
| xxxhdpi | 192 × 192 | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` |

Android Studio's **Image Asset Studio** (File → New → Image Asset) is the
easiest way to generate the full density set from a single source image.

### 4.2 Tool-tray icon (`ic_tray.png`)
Appears in ATAK's plugin tool tray. **White symbol on a transparent background**,
square, 96 × 96 px recommended. Simple silhouette shapes read best at this size.

Path: `app/src/main/res/drawable/ic_tray.png`

### 4.3 Radial menu icons
Used by the plugin's radial (long-press) menu. **White symbol on transparent
background**, 64 × 64 px PNG, one file per menu button.

Path: `app/src/main/assets/icons/<name>.png`

Name each file to match what your `WidgetItem` / radial menu factory references
in code. Any standard image editor (Inkscape, Figma, Illustrator) works; export
at 64 × 64 with a transparent background.

---

## 5. Android Studio Setup

1. **Open** the plugin project folder directly (not the SDK root).
2. **JDK:** File → Project Structure → SDK → set to JDK 17.
3. **Build Variants:** Select `civDebug` in the Build Variants panel.
4. **Run configuration:** Edit the `app` run config → Launch: **Nothing**.
   Plugins do not start as standalone apps; ATAK loads them.
5. **Sync:** If Gradle sync fails, check `docs/Build_Environment_Changes.pdf` in
   the SDK. Common cause: wrong JDK or missing `local.properties`.
6. **local.properties** (optional — only needed when using devkit repo):
   ```properties
   sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
   takrepo.url=https://artifacts.tak.gov/artifactory/maven
   takrepo.user=your_tak_username
   takrepo.password=your_tak_password
   ```
   Without `takrepo.url` the build falls back to the local `atak-sdks/` jars
   and the takdev plugin prints "Skipping civDebug" (expected in local dev).

7. **Build the debug APK:** `./gradlew assembleCivDebug`
   The signed APK lands at `app/build/outputs/apk/civ/debug/*.apk`.
   Sideload it to a device running the matching ATAK developer APK (`atak.apk`
   from the SDK root).

---

## 6. CI/CD — GitHub Actions with ATAK Version Matrix

### 6.1 Workflow structure (`.github/workflows/android.yml`)
```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  workflow_dispatch:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        atak_version: ['5.5.1', '5.6.0', '5.7.0']
      fail-fast: false      # versions are independent; run all legs regardless
    env:
      ATAK_VERSION: ${{ matrix.atak_version }}
    steps:
      - uses: actions/checkout@v4
        with:
          lfs: true         # required — SDK jars are in Git LFS

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - uses: gradle/actions/setup-gradle@v3

      - run: chmod +x gradlew

      - name: Run unit tests
        run: ./gradlew testCivDebugUnitTest

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-report-atak-${{ matrix.atak_version }}
          path: app/build/reports/tests/testCivDebugUnitTest/
          retention-days: 7

      # Generate the keystore the takdev plugin normally creates.
      # Must run before assembleCivDebug.
      - name: Generate debug signing keystore
        run: |
          mkdir -p app/build
          keytool -genkeypair -v \
            -keystore app/build/android_keystore \
            -alias wintec_mapping \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass tnttnt -keypass tnttnt \
            -dname "CN=ATAK Plugin Debug,O=Plugin,C=US" \
            -noprompt

      - name: Build debug APK
        run: ./gradlew assembleCivDebug

      - uses: actions/upload-artifact@v4
        with:
          name: debug-apk-atak-${{ matrix.atak_version }}
          path: app/build/outputs/apk/civ/debug/*.apk
          if-no-files-found: error
          retention-days: 30
```

### 6.2 Why the keystore step is needed
The takdev plugin generates `${buildDir}/android_keystore` only when the devkit
repo (`takrepo.url`) resolves. In CI it prints "Skipping civDebug" and skips all
its setup, including keystore creation. The `validateSigningCivDebug` task then
fails because the file doesn't exist. The `keytool` step re-creates it with the
credentials that match `signingConfigs.debug` in `app/build.gradle`.

### 6.3 Why `compileOnly main.jar` is needed — and why it must be conditional
The takdev plugin's flatDir fallback looks for `main.jar` at `../../main.jar`
(the SDK root) relative to the project. Locally this resolves because the project
is inside the SDK directory. In CI the checkout IS the project root, so `../../`
has nothing — every `com.atakmap.*` import fails.

The fix is to add `main.jar` from `atak-sdks/` to the compile classpath — but
**only when the devkit is not available**. TPC's pipeline runs with the devkit
enabled (`isDevKitEnabled()` returns true) and resolves the ATAK API from their
TAK maven repo. Because `atak-sdks/` is excluded from the TPC source ZIP (it
contains ~100 MB of proprietary binaries), an unconditional file reference causes
`compileCivReleaseKotlin` to fail with "File does not exist".

```groovy
if (!isDevKitEnabled()) {
    compileOnly files("${rootDir}/atak-sdks/${ATAK_VERSION}/main.jar")
    testImplementation files("${rootDir}/atak-sdks/${ATAK_VERSION}/main.jar")
}
```

- **devkit disabled** (local dev, our CI): adds the in-repo jar — compile succeeds
- **devkit enabled** (TPC's pipeline): block is skipped — maven resolution used instead

### 6.4 Testing a different ATAK version locally
```powershell
# PowerShell
$env:ATAK_VERSION = '5.6.0'
./gradlew testCivDebugUnitTest assembleCivDebug
$env:ATAK_VERSION = $null
```
```bash
# Bash / macOS / Linux
ATAK_VERSION=5.6.0 ./gradlew testCivDebugUnitTest assembleCivDebug
```
If compilation succeeds, add the version to `.github/atak-versions.json` — both
`android.yml` and `tak-tpc-release.yml` load the matrix from that file.

---

## 7. Unit Testing (JVM-only)

ATAK plugins can be unit tested on the JVM without a device using JUnit 4 + MockK.
The `returnDefaultValues = true` in `testOptions` makes Android API stubs return
null/false/0 instead of throwing `RuntimeException`.

### 7.1 Executor injection for async classes
Classes that use a background executor (e.g., file I/O) should accept it as a
constructor parameter with a default:
```kotlin
class MyStore(
    private val settings: PluginSettings,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
)
```
In tests, pass a `DirectExecutor` that runs tasks synchronously on the calling
thread, making async behaviour deterministic:
```kotlin
class DirectExecutor : AbstractExecutorService() {
    private var terminated = false
    override fun execute(command: Runnable) = command.run()
    override fun shutdown() { terminated = true }
    override fun shutdownNow() = mutableListOf<Runnable>().also { terminated = true }
    override fun isShutdown() = terminated
    override fun isTerminated() = terminated
    override fun awaitTermination(timeout: Long, unit: TimeUnit) = true
}
```

### 7.2 Mocking Kotlin final classes
MockK can mock Kotlin's final classes via ByteBuddy/Objenesis, but the class and
any properties used in tests must be declared `open`:
```kotlin
open class PluginSettings(context: Context) {
    open val imageSavePath: String get() = ...
    open val telemetryEnabled: Boolean get() = ...
}
```

---

## 8. TAK Product Center (TPC) — Signed Release APKs

ATAK plugins distributed on operational baselines must be signed by TPC.
Developers cannot sign release builds themselves; the debug keystore (ATAK dev
keystore) is used for sideloading only.

### 8.1 How TPC works
TPC runs `./gradlew assembleCivRelease` on their Gradle 6.9.1 / JDK 17 machine,
fetching the ATAK SDK from `https://artifacts.tak.gov/artifactory/maven` using
provided credentials. They apply their private signing keys and return a signed
APK + AAB.

### 8.2 Submission requirements (source ZIP)
- **Single root folder** at the zip root — the folder name becomes the APK name
  TPC produces. Name it `<repo>-<plugin-version>-atak<atak-version>`
  (e.g. `my-plugin-0.1.0-atak5.7.0`), stripping any leading `v` from
  the release tag. Including the ATAK version makes each submission unambiguous.
- **One ZIP per target ATAK version** — submit separately for each version you
  support. Each ZIP's `build.gradle` should default to its specific ATAK version
  so TPC can build without setting env vars.
- Must define the `assembleCivRelease` Gradle target.
- Must use `atak-gradle-takdev` (version `2.+` for ATAK ≥ 4.2) — TPC's maven
  repo provides it; do NOT bundle a local jar in the ZIP.
- `proguard-gradle-repackage.txt` must use your plugin name, not `PluginTemplate`.
- `AndroidManifest.xml` must contain the `com.atakmap.app.component` activity.
- **Exclude from ZIP:** `build/`, `.gradle/`, `.idea/`, `.github/`, `atak-sdks/`
  (the in-repo SDK jars), `*.apk/aar/aab`, `*.iml`, `*.DS_Store`, `.gitignore`,
  `local.properties`.

### 8.3 Automated GitHub Release workflow (`.github/workflows/tak-tpc-release.yml`)
The workflow triggers **only on `release: types: [published]`** — never on push/PR.

Supported ATAK versions are declared once in **`.github/atak-versions.json`**:
```json
["5.5.1", "5.6.0", "5.7.0"]
```
Both `tak-tpc-release.yml` and `android.yml` load this file via a `setup` job,
so adding a new ATAK version only requires editing that one file.

The packaging job runs as a matrix — one leg per ATAK version — and attaches all
ZIPs to the GitHub Release.  Key steps per leg:
```yaml
# 0. Load version matrix from the shared config file.
setup:
  outputs:
    matrix: ${{ steps.versions.outputs.matrix }}
  steps:
    - uses: actions/checkout@v4
    - id: versions
      run: echo "matrix=$(cat .github/atak-versions.json)" >> "$GITHUB_OUTPUT"

# 1. Patch the ProGuard mapping placeholder BEFORE assembleCivRelease.
- name: Patch ProGuard mapping placeholder for CI
  run: sed -i 's/-applymapping <atak.proguard.mapping>//' app/proguard-gradle.txt

- name: Verify release build compiles
  run: ./gradlew assembleCivRelease   # ATAK_VERSION env var set from matrix

# 2. Package with rsync, patch the staged build.gradle default, then zip.
- name: Package source ZIP for TPC
  run: |
    RELEASE_TAG="${{ github.event.release.tag_name }}"
    REPO_NAME="${{ github.event.repository.name }}"
    ATAK_VER="${{ matrix.atak_version }}"
    ZIP_NAME="${REPO_NAME}-TPC-Submission-${RELEASE_TAG}-atak${ATAK_VER}.zip"
    ROOT_FOLDER="${REPO_NAME}-${RELEASE_TAG#v}-atak${ATAK_VER}"
    mkdir -p "tpc-staging/${ROOT_FOLDER}"
    rsync -a \
      --exclude='.git/' --exclude='.github/' \
      --exclude='.gradle/' --exclude='.idea/' \
      --exclude='build/' --exclude='*/build/' \
      --exclude='atak-sdks/' \
      --exclude='*.apk' --exclude='*.aar' --exclude='*.aab' \
      --exclude='*.iml' --exclude='*.DS_Store' \
      --exclude='.gitignore' --exclude='local.properties' \
      . "tpc-staging/${ROOT_FOLDER}/"
    # Hard-code this leg's version as the default so TPC needs no env var.
    sed -i "s/ext\.ATAK_VERSION = .*?: '[0-9.]*'/ext.ATAK_VERSION = System.getenv('ATAK_VERSION') ?: '${ATAK_VER}'/" \
      "tpc-staging/${ROOT_FOLDER}/app/build.gradle"
    cd tpc-staging && zip -r "../${ZIP_NAME}" "${ROOT_FOLDER}/"
    echo "ZIP_NAME=${ZIP_NAME}" >> "$GITHUB_ENV"

# 3. Attach ZIP to the GitHub Release for manual TPC upload.
- name: Upload ZIP to GitHub Release
  run: gh release upload "${{ github.event.release.tag_name }}" "${{ env.ZIP_NAME }}"
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Workflow permissions required:
```yaml
permissions:
  contents: write
```

### 8.4 Manual submission
After the workflow attaches the ZIPs to the release:
1. Download each `<repo>-TPC-Submission-<tag>-atak<version>.zip` from the GitHub Release assets.
2. Log in to [tak.gov/user_builds](https://tak.gov/user_builds).
3. Upload each ZIP separately. TPC scans, compiles, signs, and returns a signed APK + AAB per submission.

### 8.5 Pre-submission verification (optional, requires tak.gov credentials)
```bash
./gradlew \
  -Ptakrepo.force=true \
  -Ptakrepo.url=https://artifacts.tak.gov/artifactory/maven \
  -Ptakrepo.user=<user> \
  -Ptakrepo.password=<pass> \
  assembleCivRelease
```
TPC will ask for the output of this command if your submission fails.

---

## 9. Release Strategy

**Use `main` as the release target** until you need to maintain multiple release
lines simultaneously (e.g., shipping patches against v0.1.x while v0.2 is in
development on `main`). At that point, branch `release/v0.1` off the `v0.1.0`
tag retroactively — Git allows this at any time. Don't pre-create release branches.

Tagging convention: `v<major>.<minor>.<patch>` (e.g., `v0.1.0`).
Publishing a GitHub Release with this tag triggers the TPC packaging workflow.

---

## 10. Common Gotchas Summary

| Symptom | Cause | Fix |
|---------|-------|-----|
| All `com.atakmap.*` imports fail in CI | takdev flatDir fallback looks for `../../main.jar`; doesn't exist in CI checkout | Add `compileOnly files("${rootDir}/atak-sdks/${ATAK_VERSION}/main.jar")` inside `if (!isDevKitEnabled())` |
| `validateSigningCivDebug FAILED` in CI | takdev "Skips" when devkit unreachable; never generates `android_keystore` | Add `keytool -genkeypair` step before APK build |
| `minifyCivReleaseWithR8` fails in CI | `-applymapping <atak.proguard.mapping>` placeholder not replaced (devkit unavailable) | `sed -i 's/-applymapping <atak.proguard.mapping>//' app/proguard-gradle.txt` before Gradle |
| `androidx.fragment` unresolved in CI | ATAK bundles fragment in `main.jar`; when `main.jar` isn't on compile classpath, fragment is also missing | Fixed by same `compileOnly main.jar` fix above |
| Plugin not loading on device | Build variant is not `civDebug`, or ATAK version on device doesn't match plugin's `atakApiVersion` manifest placeholder | Select `civDebug`; verify `ATAK_VERSION` matches installed ATAK |
| Unit tests throw on Android API calls | `returnDefaultValues = true` missing from `testOptions` | Add to `android { testOptions { unitTests { ... } } }` |
| MockK can't mock a class | Kotlin class is `final` by default | Add `open` to the class and any properties accessed in tests |
