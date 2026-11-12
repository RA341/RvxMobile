# RVxMobile

RVxMobile is an Android app to make the installing prebuilt apk releases easier.

## Latest release

- https://github.com/RA341/RvxMobile/releases/latest

- Direct APK download: https://github.com/RA341/RvxMobile/releases/latest/download/app-release.apk

If you want to see other releases or older versions, view all releases here:
https://github.com/RA341/RvxMobile/releases

## Pinned app update checks

Background checks run once a day by default. In **Settings → Pinned app updates**, choose hourly, every 6 hours, daily, weekly, or Off. Checks require a network connection; Android may delay them to save battery.

Enable notifications in the same section to receive alerts for newer releases of pinned variants installed through RvxMobile. Each release is announced once. Tap the notification to open Pinned and choose Update. Background checks do not download or install APKs automatically.

## Build from source

Prerequisites: JDK 11+, Android SDK, and Gradle.

From the project root:

```bash
# to build a release APK
./gradlew assembleRelease

# or to install on a connected device (debug build)
./gradlew installDebug
```

The release APK will be located at `app/build/outputs/apk/release/app-release.apk` after a successful build.

## Contributing

Contributions are welcome. Please open issues or pull requests with a description of the change and any testing notes.

## License


