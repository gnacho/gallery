#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const root = process.cwd();
const read = (file) => JSON.parse(fs.readFileSync(path.join(root, file), 'utf8'));
const source = read('analysis/latest/feature-diff.json');
const map = read('analysis/latest/implementation-map.json');
const assert = (condition, message) => { if (!condition) throw new Error(message); };
const originals = source.documented_additions;
assert(originals.length === 10 && map.features.length === 10, 'Ten documented additions are required');
assert(new Set(map.features.map((feature) => feature.id)).size === 10, 'Duplicate latest feature IDs');
const allowed = new Set(['verified', 'implemented', 'blocked']);
const expectedBlocked = new Set(['N007', 'N008', 'N009', 'N010']);
const blocked = new Set();
for (let index = 0; index < originals.length; index += 1) {
  const feature = map.features[index];
  assert(feature.id === originals[index].id, `Feature mismatch at ${index}`);
  assert(allowed.has(feature.status), `${feature.id}: invalid status`);
  assert(feature.implementationEvidence.length > 0, `${feature.id}: no implementation evidence`);
  assert(feature.verification.length > 0, `${feature.id}: no verification evidence`);
  for (const evidence of feature.implementationEvidence) {
    const file = path.join(root, evidence);
    assert(fs.existsSync(file) && fs.statSync(file).size > 0, `${feature.id}: missing ${evidence}`);
  }
  if (feature.status === 'verified') {
    assert(feature.implementationEvidence.some((file) => file.startsWith('app/src/test/')), `${feature.id}: verified without test source`);
    assert(feature.verification.some((command) => command.includes('--tests ')), `${feature.id}: verified without focused test`);
  }
  if (feature.status === 'blocked') {
    blocked.add(feature.id);
    assert(feature.blocker?.trim(), `${feature.id}: missing blocker`);
  }
}
assert(blocked.size === expectedBlocked.size && [...blocked].every((id) => expectedBlocked.has(id)), 'Unexpected blocked additions');
const summary = map.features.reduce((result, feature) => {
  result[feature.status] += 1;
  return result;
}, { verified: 0, implemented: 0, blocked: 0 });
assert(JSON.stringify(summary) === JSON.stringify(map.summary), 'Latest-feature summary mismatch');
for (const suiteName of ['MediaSearchTest', 'MediaNotesStoreTest', 'PlaybackSpeedTest', 'AnnualReviewTest', 'ImageEditorRendererTest']) {
  const xmlPath = path.join(root, `app/build/test-results/testDebugUnitTest/TEST-app.galeria.data.${suiteName}.xml`);
  const xml = fs.readFileSync(xmlPath, 'utf8');
  const tag = xml.match(/<testsuite\s[^>]+>/)?.[0];
  assert(tag, `${suiteName}: missing suite`);
  const value = (name) => Number(tag.match(new RegExp(`\\s${name}="(\\d+)"`))?.[1]);
  assert(value('tests') > 0 && value('failures') === 0 && value('errors') === 0 && value('skipped') === 0, `${suiteName}: tests are not clean`);
}
const apk = path.join(root, 'app/build/outputs/apk/debug/app-debug.apk');
assert(fs.existsSync(apk) && fs.statSync(apk).size > 0, 'APK missing');
execFileSync('unzip', ['-tq', apk], { stdio: 'pipe' });
console.log(`LATEST_FEATURES_CONTRACT_OK verified=${summary.verified} implemented=${summary.implemented} blocked=${summary.blocked} additions=${map.features.length}`);
