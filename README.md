# RVxMobile

RVxMobile is an Android application written primarily in Kotlin (97.7%) with a small amount of Shell scripts used for CI/build automation.

## Latest release

The README now links to the latest release automatically:

- Release page (always redirects to the latest): https://github.com/RA341/RvxMobile/releases/latest

- Direct APK download (always downloads the `app-release.apk` from the latest release): https://github.com/RA341/RvxMobile/releases/latest/download/app-release.apk

If you want to see other releases or older versions, view all releases here:
https://github.com/RA341/RvxMobile/releases

## Install (Android)

1. On your Android device, open the link above or download the `app-release.apk` to your device.
2. If installing from outside the Play Store, enable installation from unknown sources for your browser or file manager (Android settings -> Apps -> Special app access -> Install unknown apps).
3. Open the downloaded APK and follow the installer prompts.

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

Include your project license here (LICENSE file). If you don't have one yet, add a LICENSE file to the repository.
