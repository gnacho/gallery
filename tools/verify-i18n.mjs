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

import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
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

// Completitud de locales: cada clave del locale por defecto (values/) debe
// existir en cada locale extra (values-en/, ...) y con los mismos argumentos
// posicionales. Evita introducir una clave ES sin su traducción.
function localeKeys(xmlPath) {
  const txt = readFileSync(xmlPath, "utf8");
  const out = {};
  const re = /<string name="([^"]+)"[^>]*>([^<]*)<\/string>/g;
  let m;
  while ((m = re.exec(txt)) !== null) {
    out[m[1]] = (m[2].match(/%[0-9]+\$[a-z.]/g) || []).sort().join(",");
  }
  return out;
}

function missingLocales() {
  const base = join(ROOT, "app", "src", "main", "res");
  const valuesDir = join(base, "values");
  const defaultKeys = localeKeys(join(valuesDir, "strings.xml"));
  const problems = [];
  for (const e of readdirSync(base)) {
    if (!e.startsWith("values-") || !statSync(join(base, e)).isDirectory()) continue;
    const f = join(base, e, "strings.xml");
    if (!existsSync(f)) {
      problems.push(`${e}: falta strings.xml`);
      continue;
    }
    const keys = localeKeys(f);
    for (const k of Object.keys(defaultKeys)) {
      if (!(k in keys)) problems.push(`${e}: falta la clave ${k}`);
      else if (keys[k] !== defaultKeys[k]) problems.push(`${e}: args desalineados en ${k} (${keys[k]} vs ${defaultKeys[k]})`);
    }
    for (const k of Object.keys(keys)) {
      if (!(k in defaultKeys)) problems.push(`${e}: clave extra ${k}`);
    }
  }
  return problems;
}

if (list) {
  for (const f of files) console.log(f);
  console.log(`\n${violations} violaciones (modo lista, sin fallo)`);
} else {
  const localeProblems = missingLocales();
  if (violations > 0 || localeProblems.length > 0) {
    if (violations > 0) {
      console.error(`I18N_FAIL: ${violations} textos de cara al usuario hardcodeados:`);
      for (const f of files.slice(0,200)) console.error("  " + f);
    }
    if (localeProblems.length > 0) {
      console.error(`I18N_FAIL: ${localeProblems.length} desajustes de locales:`);
      for (const p of localeProblems) console.error("  " + p);
    }
    process.exit(1);
  }
  console.log("I18N_OK: 0 textos hardcodeados, locales completos y alineados");
}
