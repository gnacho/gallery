# Gates: integracion final del clon

Scope: integrar leaf-1, leaf-2, leaf-3, leaf-4, leaf-5 y leaf-6 en un unico resultado verificado.

- [x] N1: todas las hojas se reverifican desde sus ledgers exactos
  CHECK: node ~/.config/opencode/skills/unlazy/scripts/gate-check.mjs --reverify --jobs 1 .unlazy/galeria-clone/gates/leaf-1.md .unlazy/galeria-clone/gates/leaf-2.md .unlazy/galeria-clone/gates/leaf-3.md .unlazy/galeria-clone/gates/leaf-4.md .unlazy/galeria-clone/gates/leaf-6.md .unlazy/galeria-clone/gates/leaf-5.md
  EXPECT: ALL MET
  EVIDENCE: automatic-evidence=v1; definition-sha256=31661cb8817b520c7f217fddd4cdfc529cb37395db1e7f06312fbe835a47d7ab; exit=0; EXPECT=matched; output-sha256=056e09205f5a403fb2085e92cf5a9a2a4a1733fc6ebf8020765f6ff9a2169400; output-bytes=12024; shell=/bin/sh; cwd=~/Documentos/Repos/gallery-git; path=572cd79ed9f1/14 entries

- [ ] N2: no queda ninguna funcion original o anadida pendiente, bloqueada o sin verificar y todas las pantallas tienen paridad aprobada
  CHECK: node tools/verify-integration.mjs
  EXPECT: INTEGRATION_OK
  EVIDENCE: INTEGRATION_NOT_COMPLETE original=28/26/9 latest=3/3/4 visualPerfect=false
ABANDON: N2 No se puede demostrar paridad total sin los servicios privados Honor ni validacion Android de las funciones implementadas.

- [x] N3: build limpio, tests unitarios y lint terminan correctamente
  CHECK: ./gradlew clean testDebugUnitTest assembleDebug lintDebug --no-daemon
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: automatic-evidence=v1; definition-sha256=b41d17220e8022d23db7f566aadafa54655e8f09a3394afe0909b783cc60c1c9; exit=0; EXPECT=matched; output-sha256=3029b137c8e724dd1424245cdd0408fcba798670050001556c465e0998c96487; output-bytes=3789; shell=/bin/sh; cwd=~/Documentos/Repos/gallery-git; path=572cd79ed9f1/14 entries

- [x] N4: el APK final existe, es un ZIP valido y su hash queda registrado en verification/final/apk.json
  CHECK: node tools/verify-final-apk.mjs
  EXPECT: FINAL_APK_OK
  EVIDENCE: automatic-evidence=v1; definition-sha256=8c7a88cfdf0f9fb73f0bf625080e1925d07755cd2b41c2f97dd9c3e3b428ba1a; exit=0; EXPECT=matched; output-sha256=499977096c6ed925251f77851de61266fde6eb926d13485792cc9a5b1906c479; output-bytes=109; shell=/bin/sh; cwd=~/Documentos/Repos/gallery-git; path=572cd79ed9f1/14 entries

- [x] N5: todos los leases de las hojas se liberaron despues de su verificacion por el padre
  EVIDENCE: dispatch.json registra wave-1 a wave-6 complete; no se crearon leases de archivos en esta ejecucion secuencial.

- [x] N6: las evidencias manuales y las limitaciones restantes fueron revisadas a nivel de integracion
  EVIDENCE: verification/final/integration-review.md revisa matrices, capturas, bloqueos y validacion pendiente.
