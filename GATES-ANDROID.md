# Gates: validacion Android sin telefono fisico

Scope: ejecutar la app en emulator-5554 con medios sinteticos, verificar MediaStore, Compose y Media3, y conservar evidencia independiente.

- [x] A0: este ledger contiene comprobaciones que pueden fallar
  CHECK: node ~/.config/opencode/skills/unlazy/scripts/gate-lint.mjs GATES-ANDROID.md
  EXPECT: LINT OK
  EVIDENCE: automatic-evidence=v1; definition-sha256=eebf936dc7a32f85a0a4a5c4a31f41163fcfdab6ce900741b52c9d3589fa5522; exit=0; EXPECT=matched; output-sha256=48630b7361dd44ee870917b12c3d19b9d7bdea738aaca16bb04d4cab83b772d2; output-bytes=8; shell=/bin/sh; cwd=~/Documentos/Repos/gallery-git; path=572cd79ed9f1/14 entries

- [x] A1: las pruebas instrumentadas reales terminan sin fallos, errores ni omisiones en Android 17
  CHECK: ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --no-daemon && node tools/verify-android-tests.mjs
  EXPECT: ANDROID_TESTS_OK
  EVIDENCE: automatic-evidence=v1; definition-sha256=5f5bace39df15a1b272b91c22d09daf4caeffeec7e16ddb929decdf6337a9aa4; exit=0; EXPECT=matched; output-sha256=2021fd83a65c4abc0375b14b89f10bdc0b723a0aeba4a877f24ec1908bc4b441; output-bytes=5028; shell=/bin/sh; cwd=~/Documentos/Repos/gallery-git; path=572cd79ed9f1/14 entries

- [x] A2: los MP4 exportados y las capturas recuperados del emulador superan una verificacion independiente
  CHECK: node tools/collect-android-evidence.mjs && node tools/verify-android-evidence.mjs
  EXPECT: ANDROID_EVIDENCE_OK
  EVIDENCE: automatic-evidence=v1; definition-sha256=37f13372924c58a0afa4b3ae7479238182e0493831df5af862386764c5d77b76; exit=0; EXPECT=matched; output-sha256=93351929cb766060b8f4616db83132825b7f4caad77eb68708df0dd94eaf10ea; output-bytes=105; shell=/bin/sh; cwd=~/Documentos/Repos/gallery-git; path=572cd79ed9f1/14 entries

- [x] A3: los cambios no rompen los tests JVM, el build del APK ni lint
  CHECK: ./gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: automatic-evidence=v1; definition-sha256=b010a3fc1d1ca49cb426f68250ace8ab578759ba7caab4bf0c7f9b5bb8eb5f0b; exit=0; EXPECT=matched; output-sha256=2fb931c4b8340837a96f0450d608fda5a477d358199cab4cb3872c65aa805646; output-bytes=4006; shell=/bin/sh; cwd=~/Documentos/Repos/gallery-git; path=572cd79ed9f1/14 entries
