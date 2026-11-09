import fs from 'node:fs';
import crypto from 'node:crypto';

const mode = process.argv[2];
const expectedSha = '2e8a8a89d6a9910210986935e4c01f46d3d6190936b9771520e3a13492e6ee86';
const apk = '~/Descargas/Galeria-Honor.apk';
const readJson = path => JSON.parse(fs.readFileSync(path, 'utf8'));
const assert = (condition, message) => { if (!condition) throw new Error(message); };
const nonEmpty = path => fs.statSync(path).size > 0;

function verifyRea() {
  const digest = crypto.createHash('sha256').update(fs.readFileSync(apk)).digest('hex');
  assert(digest === expectedSha, 'APK digest changed');
  const pkg = readJson('analysis/original/rea/inspect-android-package.json');
  assert(pkg.subject?.digest?.sha256 === expectedSha, 'REA package target mismatch');
  assert(pkg.normalized_result?.package_name === 'com.hihonor.photos', 'Package name missing');
  assert(pkg.normalized_result?.class_count === 19899, 'Unexpected class count');
  const inventory = readJson('analysis/original/rea/class-inventory.json');
  assert(inventory.subject?.digest?.sha256 === expectedSha, 'Class inventory target mismatch');
  assert(inventory.normalized_result?.total_classes === 19899, 'Class total mismatch');
  assert(inventory.normalized_result?.matches?.length === 19899, 'Incomplete class list');
  assert(inventory.raw_result?.jar_sha256 === '6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75', 'JADX pin mismatch');
  const classes = [
    'com_hihonor_gallery_app_GalleryMain',
    'com_hihonor_gallery_recycle_app_RecycleAlbumActivity',
    'com_hihonor_photos_feature_collages_CollagesActivity',
    'com_hihonor_gallery_editor_ui_EditorUiController'
  ];
  for (const name of classes) {
    const result = readJson(`analysis/original/rea/class-${name}.json`);
    assert(result.evidence_id, `Missing class evidence: ${name}`);
    assert(result.normalized_result?.methods?.length > 0, `No methods: ${name}`);
    assert(result.subject?.digest?.sha256 === expectedSha, `Class target mismatch: ${name}`);
  }
  const signature = readJson('analysis/original/rea/inspect-apk-signature.json');
  assert(signature.subject?.digest?.sha256 === expectedSha, 'Signature target mismatch');
  assert(signature.normalized_result?.posture?.self_consistent === true, 'Invalid original signature evidence');
  console.log('ANALYSIS_REA_OK');
}

function verifyResources() {
  const decoded = readJson('analysis/original/rea/decode-android-resources.json');
  assert(decoded.normalized_result?.target?.sha256 === expectedSha, 'Resource target mismatch');
  assert(decoded.normalized_result?.decoded_file_count > 10000, 'Insufficient decoded resources');
  assert(decoded.normalized_result?.string_tables?.length >= 2, 'Missing locale string tables');
  assert(decoded.normalized_result?.manifest.includes('com.hihonor.photos'), 'Decoded manifest missing');
  const summary = readJson('analysis/original/package-summary.json');
  assert(summary.package_name === 'com.hihonor.photos', 'Summary package mismatch');
  assert(summary.component_count === 257 && summary.permissions_count === 180, 'Component or permission inventory mismatch');
  for (const path of [
    'analysis/original/apktool-decoded-fixed/AndroidManifest.xml',
    'analysis/original/apktool-decoded-fixed/res/values-es/strings.xml',
    'analysis/original/resource-feature-labels.txt',
    'analysis/original/manifest-components.txt',
    'analysis/original/screenshots/01-gallery-main-display-1.png',
    'analysis/original/screenshots/02-create-menu.png',
    'analysis/original/screenshots/04-photo-viewer.png',
    'analysis/original/screenshots/05-photo-details.png',
    'analysis/original/screenshots/01-gallery-main-controls.txt',
    'analysis/original/screenshots/02-create-menu-controls.json',
    'analysis/original/screenshots/04-photo-viewer-controls.json',
    'analysis/original/visual-spec.json'
  ]) assert(nonEmpty(path), `Missing resource evidence: ${path}`);
  for (const name of ['01-gallery-main-display-1', '02-create-menu', '04-photo-viewer', '05-photo-details']) {
    const data = fs.readFileSync(`analysis/original/screenshots/${name}.png`);
    assert(data.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10])), `Invalid PNG: ${name}`);
    assert(data.readUInt32BE(16) === 2172 && data.readUInt32BE(20) === 2352, `Unexpected capture dimensions: ${name}`);
  }
  console.log('ANALYSIS_RESOURCES_OK');
}

function verifyMatrix() {
  const matrix = readJson('analysis/original/feature-matrix.json');
  assert(matrix.source?.sha256 === expectedSha, 'Matrix source mismatch');
  assert(matrix.feature_count === matrix.features.length && matrix.features.length >= 63, 'Feature count mismatch');
  const ids = new Set();
  const names = new Set();
  const categories = new Set();
  const allowed = new Set(['pending', 'implemented', 'verified', 'blocked']);
  for (const feature of matrix.features) {
    assert(/^F\d{3}$/.test(feature.id), `Invalid id: ${feature.id}`);
    assert(!ids.has(feature.id), `Duplicate id: ${feature.id}`);
    assert(!names.has(feature.name), `Duplicate name: ${feature.name}`);
    ids.add(feature.id); names.add(feature.name); categories.add(feature.category);
    assert(feature.behavior?.length >= 30, `Missing behavior: ${feature.id}`);
    assert(feature.test?.length >= 30, `Missing test: ${feature.id}`);
    assert(allowed.has(feature.status), `Invalid status: ${feature.id}`);
    assert(Array.isArray(feature.evidence) && feature.evidence.length > 0, `Missing evidence: ${feature.id}`);
    for (const path of feature.evidence) assert(nonEmpty(path), `Invalid evidence ${feature.id}: ${path}`);
  }
  for (const required of ['F001','F012','F015','F024','F028','F032','F046','F052','F055','F060','F063']) {
    assert(ids.has(required), `Required feature absent: ${required}`);
  }
  assert(categories.size >= 15, 'Insufficient functional coverage');
  const visual = readJson('analysis/original/visual-spec.json');
  assert(visual.timeline.columns === 5 && visual.display.width_px === 2172, 'Visual specification mismatch');
  console.log('ANALYSIS_MATRIX_OK');
}

try {
  if (mode === 'rea') verifyRea();
  else if (mode === 'resources') verifyResources();
  else if (mode === 'matrix') verifyMatrix();
  else throw new Error('Usage: node tools/verify-analysis.mjs rea|resources|matrix');
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
