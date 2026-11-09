#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const root = process.cwd();
const comparison = JSON.parse(fs.readFileSync(path.join(root, 'verification/comparison.json'), 'utf8'));
const assert = (condition, message) => { if (!condition) throw new Error(message); };
assert(comparison.overallStatus === 'structural-verification-not-pixel-perfect', 'Visual status overclaims parity');
assert(comparison.viewport.width === 2172 && comparison.viewport.height === 2352, 'Wrong reference viewport');
assert(comparison.scenes.length === 4, 'Four visual scenes are required');
const expectedScenes = new Set(['photos', 'viewer', 'create', 'editor']);
for (const scene of comparison.scenes) {
  assert(expectedScenes.delete(scene.id), `Unexpected or duplicate scene ${scene.id}`);
  for (const evidence of [scene.original, scene.clone]) {
    const file = path.join(root, evidence);
    assert(fs.existsSync(file) && fs.statSync(file).size > 0, `${scene.id}: missing ${evidence}`);
    execFileSync('python3', ['-c', 'from PIL import Image; import sys; im=Image.open(sys.argv[1]); im.verify()', file], { stdio: 'pipe' });
  }
  const png = fs.readFileSync(path.join(root, scene.clone));
  assert(png.subarray(1, 4).toString() === 'PNG', `${scene.id}: clone is not PNG`);
  assert(png.readUInt32BE(16) === 2172 && png.readUInt32BE(20) === 2352, `${scene.id}: wrong capture dimensions`);
  assert(scene.corrections.length > 0, `${scene.id}: corrections not recorded`);
  assert(scene.residuals.length > 0, `${scene.id}: residual limitations not recorded`);
}
assert(expectedScenes.size === 0, 'Missing visual scenes');
const report = fs.readFileSync(path.join(root, 'verification/visual-report.md'), 'utf8');
assert(report.includes('No se declara paridad perfecta'), 'Report must reject perfect-parity claim');
assert(report.includes('No se ha instalado'), 'Device-validation boundary missing');
const xmlPath = path.join(root, 'app/build/test-results/testDebugUnitTest/TEST-app.galeria.ui.GalleryVisualTest.xml');
const xml = fs.readFileSync(xmlPath, 'utf8');
const suiteTag = xml.match(/<testsuite\s[^>]+>/)?.[0];
assert(suiteTag, 'Cannot read visual test suite');
const attribute = (name) => Number(suiteTag.match(new RegExp(`\\s${name}="(\\d+)"`))?.[1]);
const tests = attribute('tests');
assert(tests === 5, 'Expected five visual contract tests');
assert(attribute('failures') === 0 && attribute('errors') === 0 && attribute('skipped') === 0, 'Visual tests are not clean');
console.log(`VISUAL_CONTRACT_OK scenes=${comparison.scenes.length} tests=${tests} perfectParity=false`);
