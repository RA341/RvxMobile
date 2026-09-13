# RVxMobile

RVxMobile is an Android app to make the installing prebuilt apk releases easier.

## Latest release

- https://github.com/RA341/RvxMobile/releases/latest

- Direct APK download: https://github.com/RA341/RvxMobile/releases/latest/download/app-release.apk

If you want to see other releases or older versions, view all releases here:
https://github.com/RA341/RvxMobile/releases

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


