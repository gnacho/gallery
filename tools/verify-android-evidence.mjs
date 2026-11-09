import { execFileSync } from "node:child_process"
import { createHash } from "node:crypto"
import fs from "node:fs"
import path from "node:path"

const root = path.resolve(import.meta.dirname, "..")
const directory = process.argv[2]
  ? path.resolve(root, process.argv[2])
  : path.join(root, "verification/android/device")
const metadata = JSON.parse(fs.readFileSync(path.join(directory, "collection.json"), "utf8"))
if (metadata.serial !== "emulator-5554" || metadata.androidApi !== 37) throw new Error("Evidence does not come from the expected Android 17 emulator")

const files = fs.readdirSync(directory)
const sceneIds = ["01-photos", "02-details", "03-editor", "04-create"]
const photoRuns = files.map(name => /^(\d+)-01-photos\.png$/.exec(name)).filter(Boolean)
if (photoRuns.length === 0) throw new Error("No photos capture found")
const runId = photoRuns.map(match => match[1]).sort((a, b) => Number(b) - Number(a))[0]

const captures = sceneIds.map(scene => {
  const name = `${runId}-${scene}.png`
  const bytes = fs.readFileSync(path.join(directory, name))
  const signature = "89504e470d0a1a0a"
  if (bytes.subarray(0, 8).toString("hex") !== signature) throw new Error(`${name} is not a PNG`)
  const width = bytes.readUInt32BE(16)
  const height = bytes.readUInt32BE(20)
  if (width < 320 || height < 320 || bytes.length < 10_000) throw new Error(`${name} has invalid capture dimensions or size`)
  return { scene, name, width, height, bytes: bytes.length, sha256: createHash("sha256").update(bytes).digest("hex") }
})

const verifyVideo = kind => {
  const candidates = files.filter(name => new RegExp(`^\\d+-${kind}\\.mp4$`).test(name)).sort()
  if (candidates.length === 0) throw new Error(`Missing ${kind} MP4 evidence`)
  const name = candidates.at(-1)
  const result = JSON.parse(execFileSync("ffprobe", [
    "-v", "error",
    "-show_entries", "stream=codec_name,codec_type,width,height",
    "-show_entries", "format=duration",
    "-of", "json",
    path.join(directory, name),
  ], { encoding: "utf8" }))
  const stream = result.streams.find(item => item.codec_type === "video")
  const audio = result.streams.find(item => item.codec_type === "audio")
  const duration = Number(result.format.duration)
  const range = kind === "trimmed" ? [0.8, 1.3] : [1.5, 2.5]
  if (!stream || stream.width !== 96 || stream.height !== 72 || duration < range[0] || duration > range[1] || (kind === "trimmed" && !audio)) {
    throw new Error(`Invalid ${kind} MP4 metadata: ${JSON.stringify(result)}`)
  }
  return { name, codec: stream.codec_name, width: stream.width, height: stream.height, durationSeconds: duration, hasAudio: Boolean(audio) }
}

const output = {
  device: metadata.serial,
  androidApi: metadata.androidApi,
  runId,
  captures,
  videos: [verifyVideo("trimmed"), verifyVideo("slideshow")],
  visualParity: "not-assessed",
}
fs.writeFileSync(path.join(directory, "evidence.json"), `${JSON.stringify(output, null, 2)}\n`)
console.log(`ANDROID_EVIDENCE_OK captures=${captures.length} videos=2 run=${runId}`)
