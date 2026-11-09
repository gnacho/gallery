import fs from "node:fs"
import path from "node:path"
import { fileURLToPath } from "node:url"

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..")
const readJson = relative => JSON.parse(fs.readFileSync(path.join(root, relative), "utf8"))
const inventory = readJson("analysis/latest/source-inventory.json")
const diff = readJson("analysis/latest/feature-diff.json")
const matrix = readJson("analysis/original/feature-matrix.json")
const mode = process.argv[2] || "sources"
const assert = (condition, message) => {
  if (!condition) throw new Error(message)
}
const sourceIds = new Set(inventory.sources.map(source => source.id))

assert(inventory.schema === 1, "invalid source schema")
assert(inventory.package_name === "com.hihonor.photos", "wrong package")
assert(inventory.baseline.version_name === "152.0.10.794", "wrong baseline")
assert(inventory.baseline.sha256 === "2e8a8a89d6a9910210986935e4c01f46d3d6190936b9771520e3a13492e6ee86", "wrong baseline hash")
assert(inventory.candidate_status === "no-public-download-found", "candidate boundary changed")
assert(sourceIds.size === inventory.sources.length && sourceIds.size >= 12, "missing or duplicate sources")
for (const source of inventory.sources) {
  assert(source.url.startsWith("https://"), `invalid URL for ${source.id}`)
  assert(source.result && source.credibility, `incomplete source ${source.id}`)
  assert(source.provides_apk === false, `unverified APK source ${source.id}`)
}
assert(matrix.features.length === 63, "baseline matrix changed")
assert(diff.baseline.feature_count === matrix.features.length, "feature count mismatch")
assert(diff.baseline.features_preserved_by_reference === true, "baseline not preserved")

if (mode === "sources") {
  console.log("LATEST_SOURCES_OK")
} else if (mode === "candidate") {
  assert(inventory.reported_latest.version_name === "152.0.10.882", "reported version missing")
  assert(inventory.reported_latest.status === "community-report-only", "report promoted improperly")
  assert(inventory.reported_latest.apk_path === null, "unverified APK path present")
  assert(inventory.reported_latest.download_verified === false, "download marked verified")
  assert(inventory.reported_latest.signature_verified === false, "signature marked verified")
  assert(diff.candidate.binary_compared === false, "binary comparison claimed")
  const report = fs.readFileSync(path.join(root, "analysis/latest/report.md"), "utf8")
  assert(report.includes("no como candidato verificado"), "report boundary missing")
  assert(report.includes("No se descargo ni instalo software en el telefono"), "device boundary missing")
  console.log("LATEST_CANDIDATE_OK")
} else if (mode === "features") {
  const ids = new Set()
  const featureIds = new Set(matrix.features.map(feature => feature.id))
  assert(diff.documented_additions.length >= 8, "too few documented additions")
  for (const addition of diff.documented_additions) {
    assert(!ids.has(addition.id), `duplicate addition ${addition.id}`)
    ids.add(addition.id)
    assert(addition.name && addition.planned_behavior, `incomplete addition ${addition.id}`)
    assert(addition.status === "documented_external", `addition promoted improperly ${addition.id}`)
    assert(["local_candidate", "device_specific", "service_dependent"].includes(addition.implementation_class), `invalid class ${addition.id}`)
    assert(addition.source_ids.length > 0, `missing evidence ${addition.id}`)
    for (const sourceId of addition.source_ids) assert(sourceIds.has(sourceId), `unknown source ${sourceId}`)
    for (const featureId of addition.related_baseline) assert(featureIds.has(featureId), `unknown baseline feature ${featureId}`)
  }
  assert(diff.documented_additions.some(addition => addition.implementation_class === "local_candidate"), "no local additions")
  assert(diff.documented_additions.some(addition => addition.implementation_class === "service_dependent"), "service dependencies hidden")
  console.log("LATEST_FEATURES_OK")
} else {
  throw new Error(`unknown mode ${mode}`)
}
