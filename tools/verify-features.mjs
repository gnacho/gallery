#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const root = process.cwd();
const readJson = (file) => JSON.parse(fs.readFileSync(path.join(root, file), 'utf8'));
const originalDocument = readJson('analysis/original/feature-matrix.json');
const original = Array.isArray(originalDocument) ? originalDocument : originalDocument.features;
const map = readJson('analysis/implementation-map.json');
const features = map.features;
const assert = (condition, message) => { if (!condition) throw new Error(message); };

assert(original.length === 63, 'The original matrix must contain 63 features');
assert(features.length === original.length, 'Every original feature must appear exactly once');
assert(new Set(features.map((feature) => feature.id)).size === features.length, 'Duplicate feature IDs');
assert(features.every((feature, index) => feature.id === original[index].id), 'Feature order or identity differs from the original matrix');
assert(map.sourceApk.sha256 === '2e8a8a89d6a9910210986935e4c01f46d3d6190936b9771520e3a13492e6ee86', 'Source APK digest changed');

const allowed = new Set(['verified', 'implemented', 'blocked']);
const expectedBlocked = new Set(['F008', 'F017', 'F018', 'F030', 'F041', 'F052', 'F053', 'F055', 'F056']);
const actualBlocked = new Set();
for (const feature of features) {
  assert(allowed.has(feature.status), `${feature.id}: invalid or pending status ${feature.status}`);
  assert(feature.name === original.find((item) => item.id === feature.id).name, `${feature.id}: name drift`);
  assert(Array.isArray(feature.sourceEvidence) && feature.sourceEvidence.length > 0, `${feature.id}: missing source evidence`);
  assert(Array.isArray(feature.implementationEvidence) && feature.implementationEvidence.length > 0, `${feature.id}: missing implementation evidence`);
  assert(Array.isArray(feature.verification) && feature.verification.length > 0, `${feature.id}: missing verification`);
  assert(typeof feature.limitations === 'string' && feature.limitations.trim().length > 0, `${feature.id}: limitation must be explicit`);
  for (const evidence of [...feature.sourceEvidence, ...feature.implementationEvidence]) {
    const target = path.join(root, evidence);
    assert(fs.existsSync(target) && fs.statSync(target).size > 0, `${feature.id}: missing evidence ${evidence}`);
  }
  if (feature.status === 'verified') {
    assert(feature.implementationEvidence.some((file) => file.startsWith('app/src/test/') && file.endsWith('Test.kt')), `${feature.id}: verified status without a test source`);
    assert(feature.verification.some((command) => command.includes('--tests ')), `${feature.id}: verified status without a focused test command`);
  }
  if (feature.status === 'blocked') {
    actualBlocked.add(feature.id);
    assert(typeof feature.blocker === 'string' && feature.blocker.trim().length > 0, `${feature.id}: blocked status without a reason`);
  }
}
assert(actualBlocked.size === expectedBlocked.size && [...actualBlocked].every((id) => expectedBlocked.has(id)), 'Blocked feature inventory changed without updating the verifier');

const summary = features.reduce((counts, feature) => {
  counts[feature.status] += 1;
  return counts;
}, { verified: 0, implemented: 0, blocked: 0 });
assert(JSON.stringify(summary) === JSON.stringify(map.summary), 'Summary counts do not match the feature ledger');

const testDir = path.join(root, 'app/build/test-results/testDebugUnitTest');
assert(fs.existsSync(testDir), 'Missing unit test results');
const suites = fs.readdirSync(testDir).filter((file) => /^TEST-.*\.xml$/.test(file));
assert(suites.length > 0, 'No unit test suites found');
const totals = { tests: 0, failures: 0, errors: 0, skipped: 0 };
for (const suite of suites) {
  const xml = fs.readFileSync(path.join(testDir, suite), 'utf8');
  for (const key of Object.keys(totals)) {
    const match = xml.match(new RegExp(`<testsuite[^>]*\\s${key}="(\\d+)"`));
    assert(match, `${suite}: missing ${key} count`);
    totals[key] += Number(match[1]);
  }
}
assert(totals.failures === 0 && totals.errors === 0 && totals.skipped === 0, `Unit tests are not clean: ${JSON.stringify(totals)}`);
assert(totals.tests >= map.buildEvidence.tests, 'Test count regressed below the recorded baseline');
assert(suites.length >= map.buildEvidence.suites, 'Test suite count regressed');
const apk = path.join(root, 'app/build/outputs/apk/debug/app-debug.apk');
assert(fs.existsSync(apk) && fs.statSync(apk).size > 0, 'Debug APK is missing');
execFileSync('unzip', ['-tq', apk], { stdio: 'pipe' });

console.log(`FEATURES_CONTRACT_OK verified=${summary.verified} implemented=${summary.implemented} blocked=${summary.blocked} tests=${totals.tests} suites=${suites.length}`);
