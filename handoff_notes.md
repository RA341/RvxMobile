# RvxMobile Project Handoff Documentation

This handoff document provides a comprehensive overview of the **RvxMobile** architecture, file structure, and implementation details.

## App Overview

**RvxMobile** is a premium, modern Android installer and viewer built entirely in Jetpack Compose and Material 3. It simplifies the installation of non-root ReVanced and ReVanced Extended modded APKs.

### Core Workflows:
1. **Live README Sync**: Fetches the raw README.md file in real-time from the GitHub repository `FiorenMas/Revanced-And-Revanced-Extended-Non-Root`.
2. **Dynamic Parsing State-Machine**: Translates the raw Markdown elements (collapsible tags, tables, play store links) into structured Kotlin data objects.
3. **Smart ABI Matcher**: Automatically inspects the host device's CPU architecture (`Build.SUPPORTED_ABIS`) and pre-selects the recommended APK download.
4. **Foreground Service Downloader**: Processes downloads sequentially in the background using a compliant Foreground Service. The active download shows progress in a status bar notification, while completed downloads show a "Tap to Install" card.
5. **Luminance-Based WebView Renderer**: Employs a styled WebView rendering the full README using `marked.js` and official GitHub Dark/Light Markdown CSS, dynamically calculated to match the app theme's relative luminance.
6. **5-Minute Cache Retention**: Retains cache history. If a variant is downloaded and requested again within 5 minutes, it skips the network and directly launches the system Package Installer.

## Directory Structure

Below is the directory map of the newly implemented and modified files in the codebase:

```
RvxMobile/
│
├── gradle/
│   └── libs.versions.toml            # Added OkHttp, ViewModel-Compose, and Material Icons
│
└── app/
    ├── build.gradle.kts              # Declared app dependencies
    └── src/
        └── main/
            ├── AndroidManifest.xml   # Added permissions (Internet, Foreground Service, Notifications, Install Packages)
            │                         # and declared FileProvider & Foreground Service tags
            ├── res/
            │   └── xml/
            │       └── file_paths.xml # Mapped cache directories for secure FileProvider Sharing
            │
            └── java/dev/radn/rvxmobile/
                │
                ├── MainActivity.kt   # Core entry activity and bottom navigation shell
                │
                ├── data/
                │   ├── Models.kt     # AppInfo, PatcherInfo, and ApkInfo structures
                │   ├── ReadmeParser.kt # Raw markdown parser parsing sections & releases
                │   └── DownloadQueueRepository.kt # Central singleton thread-safe queue state-flow
                │
                └── ui/
                    ├── ReadmeViewModel.kt # ViewModel managing data loading and cache logic
                    ├── DownloadService.kt # Foreground service managing background download queue
                    │
                    ├── navigation/
                    │   └── Screen.kt # Bottom navigation destination enum
                    │
                    └── screens/
                        ├── DashboardScreen.kt     # Dashboard layout, search field, & dropdown selectors
                        ├── ReadmeWebViewScreen.kt # WebView rendering markdown with color-matching
                        └── DownloadsScreen.kt     # Downloads queue dashboard and progress cards
```

---

## File Purposes

### 1. Core Entry & Layout Shell
- [MainActivity.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/MainActivity.kt): Setups EdgeToEdge rendering, runs the main Scaffold, implements a bottom `NavigationBar`, collects installer prompt flows, and manages permission request hooks.
- [Screen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/ui/navigation/Screen.kt): Navigation enum categorizing the three destinations (Dashboard, Readme, Downloads) along with their corresponding icons.

### 2. User Interface screens
- [DashboardScreen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/screens/DashboardScreen.kt): Renders the searchable lists of apps. Handles expandable patcher cards containing the `ApkGroupDropdown` sub-composables.
- [DownloadsScreen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/screens/DownloadsScreen.kt): Renders the downloading dashboard showing linear progress bars for active items, queued messages, and buttons to retry failed downloads or manually trigger cache installations.
- [ReadmeWebViewScreen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/screens/ReadmeWebViewScreen.kt): WebView container configured with JS DOM Storage, loading raw markdown through marked.js. Color codes are converted to HTML Hex matching the theme's relative background luminance.

### 3. Data & State Management
- [DownloadQueueRepository.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/data/DownloadQueueRepository.kt): Central state flow container holding queue histories. It acts as the shared bridge between the background service (`DownloadService`) and the foreground UI views.
- [ReadmeParser.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/data/ReadmeParser.kt): Stateful state-machine parser parsing headers and tables to classify apps, patchers, and stable/beta release links.
- [Models.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/data/Models.kt): Data structures mapping the attributes of extracted repository elements.

### 4. Background Services & Controllers
- [DownloadService.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/DownloadService.kt): Handles background network processing. Creates notification channels, maps ongoing progress Notifications, and posts Completed Notifications containing tap-to-install PendingIntents.
- [ReadmeViewModel.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/ReadmeViewModel.kt): Fetches the raw readme URL, resolves relative paths, checks the 5-minute cache threshold, and handles intent-based installation triggers.

### 5. Manifest & Security Declarations
- [AndroidManifest.xml](file:///home/ra341/Dev/android/RvxMobile/app/src/main/AndroidManifest.xml): Declares service classes, file provider permissions, and foreground service data sync type permissions.
- [file_paths.xml](file:///home/ra341/Dev/android/RvxMobile/app/src/main/res/xml/file_paths.xml): Configures secure file provider path mappings under the `/apks/` cache folder to prevent package-parsing errors.

---

## Completed Tasks Summary

- **Task 1: Parsing Engine**: Implemented a state-machine parser parsing headers and tables to classify apps, patcher variants, Stable and Beta URLs.
- **Task 2: Separated Dropdowns**: Structured variants into distinct Stable and Beta ExposedDropdownMenuBoxes, ensuring clear visual hierarchy and pre-selecting recommended ABI matches.
- **Task 3: Cache Retention**: Established a 5-minute threshold checking logic. Fresh local copies prompt package installation instantly, saving mobile bandwidth.
- **Task 4: Background Downloader Service**: Implemented `DownloadService` running on a foreground channel, posting active progress indicators and tap-to-install completed notifications.
- **Task 5: Navigation Restructuring**: Deprecated the tabbed layout in favor of a modern M3 Bottom Navigation bar with real-time download status badges.
- **Task 6: Dynamic Light/Dark Theme Sync**: Added luminance calculations in WebView and forced dark mode as the default global theme state.
- **Task 7: Codebase Refactoring**: Cleaned up the 1000+ line MainActivity into 5 logically divided Kotlin screen and navigation files.
