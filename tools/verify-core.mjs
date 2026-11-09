import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const requiredFiles = [
  'app/src/main/java/app/galeria/MainActivity.kt',
  'app/src/main/java/app/galeria/data/MediaStoreRepository.kt',
  'app/src/main/java/app/galeria/data/TimelineGrouper.kt',
  'app/src/main/java/app/galeria/data/AlbumGrouper.kt',
  'app/src/main/java/app/galeria/ui/GalleryApp.kt',
  'app/src/main/java/app/galeria/ui/GalleryNavigation.kt',
  'app/src/test/java/app/galeria/data/MediaStoreRepositoryTest.kt',
  'app/src/test/java/app/galeria/data/TimelineGrouperTest.kt',
  'app/src/test/java/app/galeria/ui/GallerySessionTest.kt',
];
for (const file of requiredFiles) {
  if (!fs.existsSync(path.join(root, file))) throw new Error(`Missing core file: ${file}`);
}
const manifest = read('app/src/main/AndroidManifest.xml');
for (const permission of ['READ_MEDIA_IMAGES', 'READ_MEDIA_VIDEO', 'READ_MEDIA_VISUAL_USER_SELECTED']) {
  if (!manifest.includes(permission)) throw new Error(`Missing permission: ${permission}`);
}
if (!manifest.includes('android:name=".MainActivity"') || !manifest.includes('android.intent.category.LAUNCHER')) {
  throw new Error('Launcher activity is not registered');
}
const build = read('app/build.gradle.kts');
if (!build.includes('applicationId = "app.galeria"') || build.includes('com.hihonor.photos')) {
  throw new Error('Application identity is not independent');
}
const repository = read('app/src/main/java/app/galeria/data/MediaStoreRepository.kt');
for (const collection of ['MediaStore.Images.Media.EXTERNAL_CONTENT_URI', 'MediaStore.Video.Media.EXTERNAL_CONTENT_URI']) {
  if (!repository.includes(collection)) throw new Error(`Missing MediaStore collection: ${collection}`);
}
const navigation = read('app/src/main/java/app/galeria/ui/GalleryNavigation.kt');
for (const label of ['Galería', 'Fotos', 'Recuerdos', 'Crear', 'Álbumes']) {
  if (!navigation.includes(`"${label}"`)) throw new Error(`Missing destination: ${label}`);
}
const ui = read('app/src/main/java/app/galeria/ui/GalleryApp.kt');
for (const capability of ['TimelineScreen(', 'AlbumsScreen(', 'MediaViewer(', 'PermissionState(', 'LoadingState(', 'ErrorState(']) {
  if (!ui.includes(capability)) throw new Error(`Missing UI capability: ${capability}`);
}
console.log('CORE_CONTRACT_OK');
