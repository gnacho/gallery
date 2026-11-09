# Gallery

A clean-room reimplementation of the Honor gallery app (`com.hihonor.photos`),
written from scratch in Kotlin with Jetpack Compose. The original APK was
pulled from an Honor Magic V5 and analyzed locally with REA, JADX and Apktool.
This project contains no decompiled Honor code and does not reuse Honor's
package name or signing key.

## Features

The app ships as `app.galeria`, minSdk 35, targetSdk 36.

- MediaStore library with a timeline, search and notes, albums, favorites,
  trash, a private vault, a map view, manually named people, memories and an
  annual review.
- Image and video viewer with zoom, slideshow, playback speed, EXIF details,
  histogram, share, duplicate, rename and print.
- Image editor with crop, rotate, flip, filters, color adjustments, drawing,
  mosaic, text, borders, watermarks, moon composition and a magnifier.
- Collage, image sequence export to MP4 and video trim with Media3.
- A home screen widget, a `GET_CONTENT` picker and `VIEW`, `SEND`,
  `SEND_MULTIPLE` and `EDIT` entry points.

## Build

```sh
./gradlew clean testDebugUnitTest assembleDebug lintDebug --no-daemon
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --no-daemon
```

The suite runs 85 JVM tests in 45 classes and 8 instrumented tests in 5
classes on an Android 17 AVD with synthetic media only.

## Status

This is not a pixel-perfect clone. The original feature matrix records 33
verified features, 21 implemented and 9 blocked. The newer features record 3
verified, 3 implemented and 4 blocked. `verified` means behavior and build
tests passed, not full validation on a physical device or parity with
MagicOS.

Features that depend on Honor's private services are not presented as local
equivalents. That covers generative AI, Honor Cloud, proprietary casting,
private templates, Motion Photo and automatic face recognition. Emulator
captures exist in light and dark themes, but pixel parity is still unproven.
Biometrics, zoom gestures, unfolded geometry, Android 15/16 and codec or
service compatibility with MagicOS still need validation.

## License

GNU AGPL-3.0. See [LICENSE](LICENSE).
