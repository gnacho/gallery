#!/usr/bin/env node
// verify-i18n.mjs - gate determinista de i18n para el repo gallery.
//
// Detecta literales de texto "de cara al usuario" hardcodeados en Kotlin
// (en vez de stringResource) y falla si encuentra alguno. NO detecta
// strings técnicos (MIME types, claves de prefs/bundles, tags de log,
// extensiones, URLs, regex, nombres serializados) porque esos no se
// localizan.
//
// Uso: node tools/verify-i18n.mjs [--list]
//   --list  imprime las violaciones con fichero:linea y no cambia el exit code

import { readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";

const ROOT = new URL("..", import.meta.url).pathname;
const SRC = join(ROOT, "app", "src", "main", "java");

function* walk(dir) {
  for (const e of readdirSync(dir)) {
    const p = join(dir, e);
    if (statSync(p).isDirectory()) yield* walk(p);
    else if (p.endsWith(".kt")) yield p;
  }
}

// Funciones/composables propios que reciben texto visible como primer
// argumento (o primeros argumentos). Se tratan como Text().
const USER_FACING_CALLS = [
  "Text",
  "DestinationTitle",
  "MessageDestination",
  "EmptyState",
  "SnackbarHostState",
  "setTitle",
  "setSubtitle",
  "setTextViewText",
];

// Parámetros de API Compose/Android considerados "cara al usuario".
// `Text(` se trata aparte (es función, no parámetro).
const USER_FACING_PARAMS = [
  "contentDescription",
  "title",
  "subtitle",
  "label",
  "placeholder",
  "message",
  "hint",
  "supportingText",
  "headline",
  "text",
  "name",
];

// Prefijos técnicos que NO son texto localizable aunque pasen por esos
// parámetros o por Text(): no se marcan como violación.
const TECHNICAL_PATTERNS = [
  /^[a-z]+\.(?:galeria|honor|photos|gallery)$/, // applicationId/namespace
  /^image\//, /^video\//, /^audio\//,           // MIME types
  /^\w+\/[\w.+-]+$/,                             // cualquier MIME genérico
  /^https?:\/\//, /^content:\/\//, /^file:\/\//,
  /^[\w.-]+\.(?:jpg|jpeg|png|webp|gif|mp4|mkv|mov|heic|heif)$/i,
  /^[a-z_][a-z0-9_]*$/,                          // identificadores técnicos
  /^%.*$/,                                        // patrones de formato
  /^\d+$/,                                        // números
  /^[A-Z]+$/,                                     // constantes TODO/OK/etc
];

function isTechnical(s) {
  return TECHNICAL_PATTERNS.some((re) => re.test(s));
}

// Extrae literales de cadena de una línea. Cubre tanto "texto" como
// plantillas "texto ${x}" (el contenido intercalado se ignora).
function stringLiterals(line) {
  const out = [];
  // encuentra comillas dobles no escapadas
  const re = /"((?:[^"\\]|\\.)*)"/g;
  let m;
  while ((m = re.exec(line)) !== null) {
    out.push({ raw: m[0], body: m[1], col: m.index });
  }
  return out;
}

function isUserFacingLiteral(line, body) {
  // Descartar strings sin texto estático (contadores, fechas, números):
  // "3/10", "$year", "[$year]" no se localizan.
  const staticText = body
    .replace(/\$\{[^}]*\}/g, "")
    .replace(/\$\w+/g, "")
    .replace(/\[[^\]]*\]/g, "");
  if (!/[A-Za-zÁÉÍÓÚÑáéíóúñ]/.test(staticText)) return false;

  // Text("..."), DestinationTitle("..."), MessageDestination("...")...
  for (const c of USER_FACING_CALLS) {
    if (new RegExp(`\\b${c}\\s*\\(`).test(line)) return true;
  }
  // contentDescription = "...", title = "...", etc. con plantillas ${...}
  const template = body.includes("${") || body.includes("$");
  for (const p of USER_FACING_PARAMS) {
    if (new RegExp(`\\b${p}\\s*=\\s*"`).test(line)) return true;
    // plantilla interpolada: title = "${x} elementos"  (comilla despues de =)
    if (template && new RegExp(`\\b${p}\\s*=\\s*"`).test(line)) return true;
  }
  return false;
}

const list = process.argv.includes("--list");
let violations = 0;
const files = [];

for (const file of walk(SRC)) {
  const lines = readFileSync(file, "utf8").split("\n");
  lines.forEach((line, i) => {
    for (const lit of stringLiterals(line)) {
      const body = lit.body;
      if (!body.trim()) continue; // string vacío
      if (isTechnical(body)) continue;
      if (!isUserFacingLiteral(line, body)) continue;
      // descartar si ya es stringResource(...) en la misma línea
      violations++;
      files.push(`${rel(file)}:${i + 1}  ${lit.raw}`);
    }
  });
}

function rel(p) {
  return p.startsWith(SRC) ? p.slice(SRC.length + 1) : p;
}

if (list) {
  for (const f of files) console.log(f);
  console.log(`\n${violations} violaciones (modo lista, sin fallo)`);
} else {
  if (violations > 0) {
    console.error(`I18N_FAIL: ${violations} textos de cara al usuario hardcodeados:`);
    for (const f of files.slice(0, 200)) console.error("  " + f);
    process.exit(1);
  }
  console.log("I18N_OK: 0 textos de cara al usuario hardcodeados");
}
