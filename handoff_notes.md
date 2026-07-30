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
7. **Build-Time Metadata Baking**: Gradle resolved Git hash (`providers.exec` configuration-cache compliant) and build timestamps are baked directly into the app runtime as constants via `BuildConfig`.
8. **Translucent Glass Navigation**: Uses 85% opacity surface-layer translucent containers on the top navigation header and bottom bottom-navigation bar to evoke a premium obsidian glassmorphism overlay.

---

## Directory Structure

Below is the directory map of the implemented and modified files in the codebase:

```
RvxMobile/
│
├── .github/
│   └── workflows/
│       └── build.yml                 # GitHub Actions Release & Signing CI pipeline
│
├── gradle/
│   └── libs.versions.toml            # Added OkHttp, ViewModel-Compose, and Material Icons
│
├── setup_signing_secrets.sh          # Keystore generator & GitHub Secrets helper script
│
└── app/
    ├── build.gradle.kts              # Declared app dependencies, git-hash parsing, and BuildConfig fields
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
                ├── MainActivity.kt   # Core entry activity, glass navigation shell, and screen routing
                │
                ├── data/
                │   ├── Models.kt     # AppInfo, PatcherInfo, and ApkInfo structures
                │   ├── ReadmeParser.kt # Raw markdown parser parsing sections & releases
                │   └── DownloadQueueRepository.kt # Central singleton thread-safe queue state-flow
                │
                └── ui/
                    ├── ReadmeViewModel.kt # ViewModel managing data loading, cache size metrics, and cleaning logic
                    ├── DownloadService.kt # Foreground service managing background download queue
                    │
                    ├── navigation/
                    │   └── Screen.kt # Bottom navigation destination enum (Dashboard, Readme, Downloads, Settings)
                    │
                    ├── theme/
                    │   ├── Color.kt  # Custom Dark/Light palette (Electric Indigo, Teal, Obsidian, Slate)
                    │   ├── Theme.kt  # custom MaterialTheme configurations (ignoring system dynamic color)
                    │   └── Type.kt   # Geometric font sizes, line heights, and weights (Geist & Inter fallbacks)
                    │
                    └── screens/
                        ├── DashboardScreen.kt     # AppGridCard list layout and AppDetailDialog popup selector
                        ├── ReadmeWebViewScreen.kt # WebView rendering markdown with color-matching
                        ├── DownloadsScreen.kt     # Downloads queue dashboard and progress cards
                        └── SettingsScreen.kt      # Settings screen displaying git hash, build date, and cache cleaning
```

---

## File Purposes

### 1. Core Entry & Layout Shell
- [MainActivity.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/MainActivity.kt): Setups EdgeToEdge rendering, runs the main Scaffold, implements a bottom translucent `NavigationBar` (glassmorphism overlay), collects installer prompt flows, and manages navigation states.
- [Screen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/navigation/Screen.kt): Navigation enum categorizing the four destinations (Dashboard, Readme, Downloads, Settings) along with their corresponding icons.

### 2. User Interface screens
- [DashboardScreen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/screens/DashboardScreen.kt): Renders the searchable adaptive grid of apps as compact `AppGridCard` blocks. Launches `AppDetailDialog` (popup details) containing the horizontal patcher variant row pill selectors.
- [DownloadsScreen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/screens/DownloadsScreen.kt): Renders the downloading dashboard showing linear progress bars, completed task cards, and pill-shaped install buttons.
- [ReadmeWebViewScreen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/screens/ReadmeWebViewScreen.kt): WebView container configured with JS DOM Storage, loading raw markdown through marked.js.
- [SettingsScreen.kt](file:///home/ra341/Dev/android/RvxMobile/app/src/main/java/dev/radn/rvxmobile/ui/screens/SettingsScreen.kt): Renders the settings panels displaying the baked Git Commit Hash (with copy action) and compilation date/time, alongside internal storage cache metrics with a working "Clear Cache" button.

### 3. Data & State Management
- [DownloadQueueRepository.kt](file:///home/ra341/Dev/android/RvxMobile/data/DownloadQueueRepository.kt): Central state flow container holding queue histories.
- [ReadmeParser.kt](file:///home/ra341/Dev/android/RvxMobile/data/ReadmeParser.kt): Stateful state-machine parser parsing headers and tables to classify apps, patchers, and stable/beta release links.
- [Models.kt](file:///home/ra341/Dev/android/RvxMobile/data/Models.kt): Data structures mapping the attributes of extracted repository elements.

### 4. Background Services & Controllers
- [DownloadService.kt](file:///home/ra341/Dev/android/RvxMobile/ui/DownloadService.kt): Handles background network downloads. Creates notification channels and maps ongoing progress.
- [ReadmeViewModel.kt](file:///home/ra341/Dev/android/RvxMobile/ui/ReadmeViewModel.kt): Fetches the raw readme, handles cache files checks, and manages context directory size calculation and storage cleaning triggers.

### 5. Automation & Key Signing Configuration
- [build.yml](file:///home/ra341/Dev/android/RvxMobile/.github/workflows/build.yml): Compile release targets (`assembleRelease`), decode Base64 signing certificates from GitHub Secrets, sign the release build using `apksigner`, tag the release based on current push timestamp, and push asset directly to GitHub Releases (fails if secrets are missing).
- [setup_signing_secrets.sh](file:///home/ra341/Dev/android/RvxMobile/setup_signing_secrets.sh): Generates local release keystore, encodes the keystore to base64, and uploads all credentials as GitHub Repository Secrets using the `gh` CLI.

---

## Completed Tasks Summary

- **Task 1: Parsing Engine**: Implemented a state-machine parser parsing headers and tables to classify apps, patcher variants, Stable and Beta URLs.
- **Task 2: Separated Dropdowns**: Structured variants into distinct Stable and Beta ExposedDropdownMenuBoxes, ensuring clear visual hierarchy and pre-selecting recommended ABI matches.
- **Task 3: Cache Retention**: Established a 5-minute threshold checking logic. Fresh local copies prompt package installation instantly.
- **Task 4: Background Downloader Service**: Implemented `DownloadService` running on a foreground channel, posting active progress indicators.
- **Task 5: Navigation Restructuring**: Implemented Bottom Navigation bar (Dashboard, Readme, Downloads, Settings) with translucent glassmorphism container styling.
- **Task 6: Dynamic Light/Dark Theme Sync**: Added luminance calculations in WebView and forced dark mode as the default global theme state.
- **Task 7: Codebase Refactoring**: Cleaned up the 1000+ line MainActivity into logically divided screen files.
- **Task 8: Build-time Metadata baking**: Set up Gradle scripts to capture Git commit hash (`providers.exec`) and compilation time at build time.
- **Task 9: Design System Integration**: Implemented DESIGN.md specifications including custom brand colors (Teal, Electric Indigo, Charcoal), shapes, corner radii, and fully pill-shaped buttons.
- **Task 10: Horizontal Pill Patcher Selector**: Refactored stacked patchers inside apps into horizontal scrollable pill selector rows.
- **Task 11: Compact Grid List & Overlay Dialogs**: Converted main list layout to an adaptive grid of compact cards, opening a bounded scrollable detail dialog on tap.
- **Task 12: GitHub CI/CD Release Pipeline**: Set up automated GitHub releases with signed release APKs and repository secret credentials.
