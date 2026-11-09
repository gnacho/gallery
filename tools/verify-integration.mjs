#!/usr/bin/env node
import fs from 'node:fs';

const read = (file) => JSON.parse(fs.readFileSync(file, 'utf8'));
const original = read('analysis/implementation-map.json');
const latest = read('analysis/latest/implementation-map.json');
const visual = read('verification/comparison.json');
const plan = fs.readFileSync('.unlazy/galeria-clone/PLAN.md', 'utf8');
const leafStates = [...plan.matchAll(/\| (leaf-[1-6]) \|[^\n]+\| (VERIFIED|IN-FLIGHT|WAITING) \|/g)];
if (leafStates.length !== 6 || leafStates.some((match) => match[2] !== 'VERIFIED')) {
  throw new Error('Not all six leaves are VERIFIED');
}
const originalPending = original.features.filter((feature) => feature.status !== 'verified');
const latestPending = latest.features.filter((feature) => feature.status !== 'verified');
const visualPerfect = visual.overallStatus === 'pixel-perfect-verified';
if (originalPending.length || latestPending.length || !visualPerfect) {
  console.error(`INTEGRATION_NOT_COMPLETE originalVerified=${original.summary.verified} originalImplemented=${original.summary.implemented} originalBlocked=${original.summary.blocked} latestVerified=${latest.summary.verified} latestImplemented=${latest.summary.implemented} latestBlocked=${latest.summary.blocked} visualPerfect=${visualPerfect}`);
  process.exit(1);
}
console.log('INTEGRATION_OK');
