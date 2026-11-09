#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';

const root = process.cwd();
const source = path.join(root, 'app/build/outputs/apk/debug/app-debug.apk');
const outDir = path.join(root, 'verification/final');
const target = path.join(outDir, 'Galeria.apk');
if (!fs.existsSync(source) || fs.statSync(source).size === 0) throw new Error('Debug APK is missing');
fs.mkdirSync(outDir, { recursive: true });
fs.copyFileSync(source, target);
execFileSync('unzip', ['-tq', target], { stdio: 'pipe' });
const buildTools = '~/Android/Sdk/build-tools/37.0.0';
execFileSync(path.join(buildTools, 'apksigner'), ['verify', target], { stdio: 'pipe' });
const badging = execFileSync(path.join(buildTools, 'aapt2'), ['dump', 'badging', target], { encoding: 'utf8' });
if (!badging.includes("package: name='app.galeria'")) throw new Error('Unexpected application ID');
const bytes = fs.readFileSync(target);
const sha256 = createHash('sha256').update(bytes).digest('hex');
let totals = { tests: 0, failures: 0, errors: 0, skipped: 0 };
const testDir = path.join(root, 'app/build/test-results/testDebugUnitTest');
const suites = fs.readdirSync(testDir).filter((file) => /^TEST-.*\.xml$/.test(file));
for (const suite of suites) {
  const xml = fs.readFileSync(path.join(testDir, suite), 'utf8');
  const tag = xml.match(/<testsuite\s[^>]+>/)?.[0];
  if (!tag) throw new Error(`Invalid test suite ${suite}`);
  for (const key of Object.keys(totals)) {
    const value = tag.match(new RegExp(`\\s${key}="(\\d+)"`))?.[1];
    if (value === undefined) throw new Error(`Missing ${key} in ${suite}`);
    totals[key] += Number(value);
  }
}
if (totals.failures || totals.errors || totals.skipped) throw new Error(`Unclean tests ${JSON.stringify(totals)}`);
const original = JSON.parse(fs.readFileSync(path.join(root, 'analysis/implementation-map.json'), 'utf8'));
const latest = JSON.parse(fs.readFileSync(path.join(root, 'analysis/latest/implementation-map.json'), 'utf8'));
const record = {
  file: 'verification/final/Galeria.apk',
  applicationId: 'app.galeria',
  versionName: '0.1.0',
  versionCode: 1,
  minSdk: 35,
  targetSdk: 36,
  sizeBytes: bytes.length,
  sha256,
  signatureVerified: true,
  originalApkSha256: original.sourceApk.sha256,
  tests: { ...totals, suites: suites.length },
  androidTests: JSON.parse(fs.readFileSync(path.join(root, 'verification/android/tests.json'), 'utf8')),
  originalFeatures: original.summary,
  latestFeatures: latest.summary,
  perfectClone: false,
  generatedAt: new Date().toISOString(),
};
fs.writeFileSync(path.join(outDir, 'apk.json'), `${JSON.stringify(record, null, 2)}\n`);
console.log(`FINAL_APK_OK sha256=${sha256} bytes=${bytes.length} tests=${totals.tests}`);
