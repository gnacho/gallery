import fs from "node:fs"
import path from "node:path"

const root = path.resolve(import.meta.dirname, "..")
const directory = path.join(root, "app/build/outputs/androidTest-results/connected/debug")
const files = fs.readdirSync(directory).filter(name => /^TEST-.*\.xml$/.test(name))
if (files.length === 0) throw new Error("No Android test XML files found")

const totals = { tests: 0, failures: 0, errors: 0, skipped: 0 }
const suites = []
const devices = new Set()
for (const file of files) {
  const xml = fs.readFileSync(path.join(directory, file), "utf8")
  for (const match of xml.matchAll(/<testsuite\b[^>]*>/g)) {
    const attributes = Object.fromEntries(Array.from(match[0].matchAll(/([\w-]+)="([^"]*)"/g), item => [item[1], item[2]]))
    for (const key of Object.keys(totals)) totals[key] += Number(attributes[key] ?? 0)
    suites.push(attributes.name)
  }
  for (const match of xml.matchAll(/<property name="device" value="([^"]+)"\s*\/>/g)) devices.add(match[1])
}

const expectedSuites = [
  "app.galeria.data.Media3InstrumentedTest",
  "app.galeria.data.MediaStoreInstrumentedTest",
  "app.galeria.data.VaultInstrumentedTest",
  "app.galeria.data.VideoPlaybackInstrumentedTest",
  "app.galeria.ui.GalleryUiInstrumentedTest",
]
if (totals.tests < 8) throw new Error(`Expected at least 8 tests, found ${totals.tests}`)
if (totals.failures !== 0 || totals.errors !== 0 || totals.skipped !== 0) throw new Error(`Unexpected Android test results: ${JSON.stringify(totals)}`)
if (!expectedSuites.every(name => suites.includes(name))) throw new Error(`Missing Android suites: ${JSON.stringify(suites)}`)
if (devices.size !== 1 || !devices.has("emulator-5554")) throw new Error(`Unexpected devices: ${Array.from(devices).join(",")}`)

const output = {
  device: "emulator-5554",
  androidApi: 37,
  ...totals,
  suites: suites.sort(),
}
fs.mkdirSync(path.join(root, "verification/android"), { recursive: true })
fs.writeFileSync(path.join(root, "verification/android/tests.json"), `${JSON.stringify(output, null, 2)}\n`)
console.log(`ANDROID_TESTS_OK tests=${totals.tests} suites=${suites.length} failures=0 errors=0 skipped=0`)
