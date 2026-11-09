import { execFileSync } from "node:child_process"
import fs from "node:fs"
import path from "node:path"

const serial = "emulator-5554"
const root = path.resolve(import.meta.dirname, "..")
const target = process.argv[2]
  ? path.resolve(root, process.argv[2])
  : path.join(root, "verification/android/device")
fs.rmSync(target, { recursive: true, force: true })
fs.mkdirSync(target, { recursive: true })
execFileSync("adb", ["-s", serial, "pull", "/sdcard/Download/GaleriaVerification/.", `${target}/`], { stdio: "pipe" })

const collected = []
for (const file of fs.readdirSync(target)) {
  if (!file.endsWith(".capture")) continue
  const output = file.endsWith(".mp4.capture")
    ? file.slice(0, -".capture".length)
    : `${file.slice(0, -".capture".length)}.png`
  fs.renameSync(path.join(target, file), path.join(target, output))
  collected.push(output)
}
if (collected.length === 0) throw new Error("No evidence files recovered")

const deviceProperty = property => execFileSync("adb", ["-s", serial, "shell", "getprop", property], { encoding: "utf8" }).trim()
const metadata = {
  serial,
  model: deviceProperty("ro.product.model"),
  androidApi: Number(deviceProperty("ro.build.version.sdk")),
  androidRelease: deviceProperty("ro.build.version.release"),
  collectedAt: new Date().toISOString(),
  files: collected.sort(),
}
fs.writeFileSync(path.join(target, "collection.json"), `${JSON.stringify(metadata, null, 2)}\n`)
console.log(`Recovered ${collected.length} evidence files from ${serial}`)
