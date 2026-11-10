# DESIGN.md

Design system for the Gallery app (clean-room reimplementation of the Honor
Gallery, Kotlin + Jetpack Compose, Material 3).

The observable Honor Gallery interface is the binding visual reference. The app
uses Material 3 structure and components, themed through Material color roles,
type scale, and a left navigation rail.

## Color

Two static fallback schemes plus Material You dynamic color.

- Dynamic color (Android 12+): derived from the user wallpaper via
  `dynamicDarkColorScheme` / `dynamicLightColorScheme`.
- Static fallback (dynamic color off):

  | Role | Dark | Light |
  |---|---|---|
  | primary | `#6B9BFF` | `#245FC7` |
  | onPrimary | white | white |
  | background | black | `#FAFAFC` |
  | onBackground | white | `#17181B` |
  | surface | black | `#FAFAFC` |
  | onSurface | white | `#17181B` |
  | surfaceVariant | `#17181B` | `#E8EAF0` |
  | onSurfaceVariant | `#C8CAD0` | `#454852` |

Rules:

- Screens use Material color roles (`colorScheme.*`). No raw hex for semantic
  colors: error text uses `colorScheme.error`, secondary text uses
  `onSurfaceVariant`, container surfaces use `surfaceVariant`.
- Translucent scrims (e.g. `Color(0x88000000)`) are allowed over media to keep
  text legible on arbitrary image content.
- The media viewer and vault preview are intentionally black with white text.

## Typography

Material 3 type scale; no hand-picked `fontSize`.

- Screen titles: `headlineLarge` (weight Medium).
- Message/empty-state titles: `headlineMedium`.
- Panel and card titles: `titleLarge` (weight SemiBold).
- Section headers: `titleMedium` (weight SemiBold).
- Body and secondary text: `bodyMedium` / `bodySmall`.
- Labels, counts, metadata: `labelLarge` / `labelMedium` / `labelSmall`.
- All sizes in `sp`, so text follows the system font scale.

## Navigation

- A left navigation rail (`GalleryRail`): 80 dp on compact, 160 dp on expanded
  (width >= 600 dp). Destinations: Gallery, Photos, Memories, Create, Albums,
  plus Settings.
- System Back is honored everywhere (`BackHandler`), including predictive back.
- Edge-to-edge with `systemBarsPadding` so content never sits under system bars.

## Components

- Material 3: `IconButton`, `TextButton`, `Button`, `OutlinedTextField`,
  `AlertDialog`, `ModalBottomSheet`, `DropdownMenu`, `Snackbar`, `Slider`,
  `FilterChip`, `Switch`, `Card`.
- Material outlined icons, one consistent weight (`GalleryIcons`).
- One primary action per screen; a single FAB is not used (the gallery has no
  single primary creation action on its main screens).

## States

Every screen has loading, error, and empty states (`LoadingState`,
`ErrorState`, `EmptyState`, `MessageDestination`). Transient feedback uses
snackbars; destructive confirmations use dialogs.

## Accessibility

- Interactive elements are labeled: every icon carries a `contentDescription`
  (localized), decorative icons pass `null`.
- Touch targets are Material `IconButton` / `TextButton` size (48 dp minimum).
- Text scales with the system font size (`sp`).
- Color contrast uses `onSurfaceVariant` / `onBackground` roles tuned for both
  themes; scrims ensure text over media stays readable.
- Dark theme is a first-class scheme, not an invert.

## Motion

- Fade-through (`Crossfade`) on the top-level content states (permission,
  loading, error, content).
- System-provided component motion: snackbar, dropdown menu, bottom sheet.
- Not yet: shared-axis / container-transform for the media viewer open/close.
  Honors the system "Remove animations" setting through Compose defaults.
