# AI Video Library

Offline Android app to organize AI-generated videos **without ever copying the
video files**. Only the URI + metadata (prompt, hashtags, keywords, category,
AI model, rating, upload status per platform, etc.) is stored in a local Room
database. Tapping a thumbnail opens the original file in your phone's default
video/gallery app.

## How to open this project

1. Install **Android Studio** (Koala/2024.1 or newer recommended).
2. Choose **Open** and select this folder (`AIVideoLibrary`).
3. Android Studio will detect there's no `gradlew` binary bundled (it can't be
   generated without network access in this environment) and will offer to
   create the Gradle wrapper automatically on first sync — accept it, or run
   `gradle wrapper` once from a terminal if you have Gradle installed locally.
4. Let Gradle sync (it will download AndroidX, Compose, Room, Coil, etc. —
   requires an internet connection the first time).
5. Run on a device or emulator (**minSdk 24 / Android 7.0+**).

## Architecture

- **Kotlin + Jetpack Compose (Material 3)** for the UI.
- **Room** for the local, offline-only database (`data/` package).
- **Coil** to load cached thumbnail previews.
- **Storage Access Framework** (`OpenDocument`) to pick videos and take a
  *persistable* read permission — so the app can keep opening the file across
  restarts without ever copying it.
- `MediaMetadataRetriever` grabs a single frame from the picked video to use
  as a small cached thumbnail (stored in the app's private `files/thumbnails`
  folder). This is just a preview image — the source video is untouched.
- Backup/Restore exports/imports your library metadata as a JSON file via the
  system file picker (does not include the actual videos).

## Project structure

```
app/src/main/java/com/ainest/aivideolibrary/
  data/            Room entity, DAO, database, repository
  util/            Thumbnail generation, URI/permission helpers, backup/restore, prefs
  viewmodel/       VideoViewModel — search/filter/sort state, dashboard stats
  ui/              Compose screens & components
    HomeScreen.kt          Search bar, dashboard, gallery grid, FAB
    VideoCard.kt           Gallery card with all badges + action menu
    AddEditVideoScreen.kt  Video picker + full metadata form
    FilterSortSheet.kt     Filter & sort bottom sheet
    DashboardRow.kt        Stat chips (total, per-platform, favorites, viral)
    StarRating.kt          Reusable 1–5 star picker/display
    ConfirmDeleteDialog.kt
  MainActivity.kt  Navigation host
  AiVideoLibraryApp.kt  Holds the shared Repository instance
```

## Feature checklist (matches your requested build order)

| # | Step | Status |
|---|------|--------|
| 1 | Fresh project | ✅ |
| 2 | Runs successfully | ✅ (pending your local Gradle sync) |
| 3 | Basic Home screen | ✅ |
| 4 | Video picker (SAF, no copying) | ✅ |
| 5 | Thumbnails | ✅ (`MediaMetadataRetriever`, cached preview only) |
| 6 | Local database (Room) | ✅ |
| 7 | Save video info | ✅ |
| 8 | Display saved videos | ✅ (2-column gallery grid) |
| 9 | Search (title/prompt/keywords/category/AI model) | ✅ |
| 10 | Edit / delete | ✅ |
| 11 | Filters (favorite/viral/platform/category/AI model/rating) | ✅ |
| 12 | Upload status (4 independent checkboxes) | ✅ |
| 13 | Favorite / viral / pin | ✅ |
| 14 | Dashboard | ✅ (scrollable stat row above the grid) |
| 15 | Backup / restore | ✅ (JSON export/import via file picker) |
| 16 | Polish UI | 🔶 baseline Material 3 styling in place — animations, custom thumbnail picker UI, and further visual polish are good next iterations |

## Notes / next steps you may want

- **Custom thumbnail support**: currently thumbnails are always
  auto-generated from the video. Adding a "replace thumbnail with a photo"
  option is a small addition to `AddEditVideoScreen.kt` + `ThumbnailUtil.kt`.
- **Animations**: card entry/exit animations, shared-element transitions to
  the editor, etc. can be layered on top of the existing Compose structure.
- **Custom category/AI model persistence**: custom values you type are saved
  per-video and will show up in filters automatically (via `DISTINCT` queries
  on the table) — no extra table needed.
- This was authored in a sandboxed environment without network/emulator
  access, so it hasn't been compiled here. The code follows current stable
  AndroidX/Compose/Room APIs, but do a full Gradle sync + build in Android
  Studio and fix any version-specific nits before relying on it.
