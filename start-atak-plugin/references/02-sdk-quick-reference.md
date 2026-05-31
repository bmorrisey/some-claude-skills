# ATAK Plugin Development Instructions (SDK 5.7.0.5)

This guide highlights the high-priority files within the SDK to streamline plugin development in a plugin, for example in `plugins/my-plugin/` and troubleshoot environment issues.

---

## QUICK REFERENCE FOR SDK FILE STRUCTURE

### 1. Essential Documentation & API Reference
*   **`ATAK_Plugin_Development_Guide.pdf` (Root):** The "Source of Truth." Consult this for the plugin lifecycle, `DropDownReceiver` logic, and map interaction patterns.
*   **`atak-javadoc.jar` (Root):** Crucial for method signatures. Link this in Android Studio to see documentation on hover.
*   **`docs/Build_Environment_Changes.pdf`:** Review this first if the project fails to sync/build on a newer SDK version.
*   **`docs/dependencies.txt`:** Check this to see which libraries are already provided by ATAK core to avoid "Duplicate Class" Gradle errors.

### 2. Boilerplate & Reference Samples
When building `plugins/my-plugin/`, prioritize these for "copy-paste" logic:
*   **`samples/plugintemplate/`:** The primary modern starting point. 
*   **`samples/PluginTemplateLegacy/`:** Use if the developer prefers the older `MapComponent` / `DropDownReceiver` split.
*   **`samples/helloworld/`:** The absolute minimum configuration for a "button-on-map" plugin.
*   **`samples/plugintemplate-compose/`:** Reference this if the developer wants to use Jetpack Compose for the UI.

### 3. Build Configuration & Troubleshooting
*   **`atak-gradle-takdev.jar` (Root):** The custom Gradle plugin that handles plugin packaging. If Gradle tasks like `assembleCivDebug` are missing, this jar isn't being loaded.
*   **`samples/plugintemplate/template.local.properties`:** Copy this to `plugins/my-plugin/local.properties`. This is where you define the SDK path and `takrepo` credentials.
*   **`main.jar` (Root):** The primary library your plugin compiles against.
*   **`VERSION.txt` (Root):** Ensure the `plugin.xml` version matches the first two digits of this file (e.g., `5.7`).

### 4. Deployment & Testing
*   **`atak.apk` (Root):** The specific developer-build of ATAK. You **must** install this on the emulator/device; the Play Store version will reject your custom-signed plugin.
*   **`android_keystore` (Root):** The default debug signing key. Ensure the plugin and the dev APK are signed with compatible keys.
*   **`espresso/`:** Reference `testSetup.gradle` if setting up automated UI testing for the plugin.

---

### Workflow for AI Agent:
1.  **Syncing:** Ensure `plugins/my-plugin/local.properties` points `sdk.dir` to the SDK root.
2.  **Coding:** Reference `main.jar` via the `plugintemplate` build logic. Use `com.atakmap.core.util.Log` for debugging.
3.  **UI:** Inflate layouts from `res/layout` and manage them via a `DropDownReceiver`.
4.  **Debugging Build Errors:** Check `docs/Build_Environment_Changes.pdf` for recent Gradle requirement shifts (e.g., JDK 17 vs JDK 11).

---

## 1. Environment Setup & Prerequisites
*   **IDE:** Android Studio (latest stable version, e.g., Narwhal 2025.1.1+).
*   **Java:** Java 11 or the bundled Android Studio Java version.
*   **Gradle:** 
    *   Gradle Plugin Version: `8.9.0`
    *   Gradle Version: `8.13`
*   **Target Device:** A physical Android device or emulator with the **Developer Build** of ATAK-CIV installed. This APK is found within the SDK ZIP file (`atak-civ-sdk-5.5.x.zip`).

## 2. Project Initialization
To start a new plugin using the SDK:
1.  **Directory Structure:** Create a folder named `plugins` at the root level of the unzipped ATAK SDK.
2.  **Template Selection:** Copy a template (e.g., `plugintemplate` or `helloworld`) from the SDK’s `samples` or `plugin-examples` directory into your new `plugins` folder.
3.  **Build Variant:** In Android Studio, navigate to the **Build Variants** tab and change the Active Build Variant to **`civDebug`**.
4.  **Run Configuration:**
    *   Edit the `app` configuration.
    *   Under **Launch Options**, set **Launch** to **`Nothing`**. ATAK plugins do not launch as standalone activities; they are loaded by the core ATAK application.

## 3. Plugin Architecture (SDK 5.5+)
The SDK 5.5 supports two primary styles. When assisting the developer, clarify which version they are using:

### A. Legacy Style (Most Common for Stability)
*   **`MapComponent`:** The entry point where the plugin is registered and initialized.
*   **`DropDownReceiver`:** Manages the "Side Pane" or "Pull-out" menu UI.
*   **`Lifecycle`:** Plugins use `onReceive` and `onDispose` to manage resources.

### B. New Plugin Style (SDK 5.5+)
*   **`PluginTemplate` Class:** Replaces the need for separate `MapComponent` and `DropDownReceiver` classes.
*   **UI Services:** Uses `IHostUIService` to initialize buttons and toolbar items.
*   **Pane Builder:** Uses a `PaneBuilder` to define the size, position, and metadata of the plugin's UI window (e.g., setting `Preferred_Width_Ratio` to `0.5`).

## 4. UI Development
*   **Layouts:** Defined in `res/layout` using traditional Android XML.
*   **Compose:** While SDK 5.5 introduces a Compose template, it is currently noted as unstable in some environments. Stick to XML/Views unless the developer specifically requests Compose.
*   **Components:**
    *   **Toolbar Buttons:** Use `ToolbarItem.Builder` to add your plugin icon to the top ATAK ribbon.
    *   **Side Panes:** Use `PluginLayoutInstaller` to inflate XML layouts into the ATAK side menu.

## 5. Integrating Custom Logic (Example: Drone/Telemetry)
If the plugin requires real-time data (like the MavSDK drone example):
1.  **Dependencies:** Add necessary libraries (e.g., `io.mavsdk:mavsdk`) to `build.gradle`.
2.  **Concurrency:** Use **Kotlin Coroutines** and **StateFlows** to handle background data updates without freezing the Map UI.
3.  **Networking:** ATAK architecture supports Client-Server (TAKServer) or Mesh (GoTenna). If using local telemetry, ensure UDP/TCP ports (e.g., `14550` for Mavlink) are properly configured and open in any firewalls.
4.  **Map Interaction:** Use the `MapView` object provided in the plugin context to place markers or overlays programmatically.

## 6. Development Workflow
1.  **Convert to Kotlin:** Most templates are Java-based. The AI should offer to convert the template's `MapComponent` and `DropDownReceiver` to Kotlin for modern development.
2.  **Logging:** Use `com.atakmap.core.util.Log` for debugging. These logs appear in the Android Studio Logcat under the `TAK` tag.
3.  **Deployment:**
    *   Build the APK in Android Studio.
    *   The plugin must be loaded within the ATAK app under **Settings > Tool Management > App Management**.
    *   The plugin **Package Name** must follow the standard naming convention to be recognized.

## 7. Troubleshooting Common Issues
*   **Plugin Not Loading:** Verify the `civDebug` build variant is selected and that the version of the SDK matches the version of ATAK installed on the device.
*   **UI Not Appearing:** Ensure the `ToolbarItem` is correctly registered in the `onCreate` or `onReceive` methods.
*   **Crashes on Launch:** Check if the `Launch Nothing` option was set in the Run Configuration; otherwise, Android tries to launch the plugin as a standalone app.