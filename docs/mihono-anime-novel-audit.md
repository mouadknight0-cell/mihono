# Mihono — Anime/Novel removal audit

Mihono is a Manhwa/Manga-focused fork. This document records what has been
removed, what remains, and why the remainder is not deleted blindly.

## Removed (user-facing entry points)

- Bottom navigation: Anime library, Novel library, and Updates tabs
  (`NavTab` now only Manga / History / Browse; `StartScreen` Manga default).
- Anime/Novel sub-tabs in Browse, History, and (removed) Updates containers;
  Browse now shows Manga sources / extensions / migrate only.
- Anime and Novel dedicated library screens (`AnimeLibraryTab`,
  `NovelLibraryTab`), `UpdatesTab`, anime History/Updates sub-tabs, and the
  novel Browse tabs (`NovelSourceTabs`).
- Settings entry points: anime & novel extension-repo settings, the
  "hide in anime library" switch, and the video Player settings entries
  (Settings main list and More tab).
- Global search: the "Pinned" source filter and its persisted preference, and
  novel-only search scoping. The "Has results" filter remains.
- `HomeTabsMode` preference (anime/manga sub-tab mode) and its settings UI.

## Remaining, by category

### 1. Anime-only, removable in a dedicated follow-up (currently unreachable or settings-orphaned UI)

- `ui/browse/anime/**` (33 files), `presentation/browse/anime/**`
- `ui/entries/anime/**`, `presentation/entries/anime/**`
- `ui/library/anime/**` (screen models/items), anime presentation library UI
- `presentation/more/settings/screen/browse/AnimeExtensionReposScreen(.Model)`,
  novel equivalent
- Anime history/updates screen models under `ui/history/anime`, `ui/updates/anime`

These compile today and some are still referenced from inside the anime
cluster (e.g. `AnimeExtensionsTab` references the anime repos screen) and from
a few shared hosts (`DownloadsTab` anime queue, `CategoriesTab` anime
categories, `StatsTab`, notifications, deeplink/player). They must be removed
together with those hosts' anime sections, with a CI build after each step —
not by keyword deletion.

### 2. Anime infrastructure that shared code still touches (do not delete casually)

- Player stack `ui/player/**` (61 files): referenced by `MainActivity`
  (`startPlayerActivity`, saved episode state), `NotificationReceiver`, and
  external-intent handling.
- Anime domain/data: `domain/**/anime/**`, `data/**/anime/**`,
  `eu.kanade.domain/**/anime/**`, download manager/store, library update jobs,
  trackers (Anilist etc.), backup creators/restorers, SQLDelight anime tables
  and migrations. Backup/restore and database migrations must keep reading old
  backups/schemas; deleting tables would break upgrades.
- DI registrations in `AppModule`/`DomainModule` for the above.

### 3. Novel code paths (shared with manga, keep)

Novels were implemented *on top of* the manga stack (`NovelSourceCompat`,
`LocalNovelSource`, `novelOnly` flags defaulting to `false`). With the Novel UI
gone these flags are inert defaults; the local manga source, manga downloads
(which branch on `LocalNovelSource` for local files), reader text loader, and
extension loader still reference them. Removing them is a separate refactor of
the manga pipeline, not a tab deletion.

### 4. Shared components that must remain

Manga library/browse/search/reader/downloads, `TabbedScreen`, categories,
stats, storage, backup framework, and all migrations — used by the manga
experience regardless of anime/novel code.

## Rule for further removal

Remove category 1 in small commits (UI cluster + its shared-host references),
run the PR CI (spotless + `assembleRelease` + `testReleaseUnitTest`) after each,
and only then evaluate category 2. Category 3 requires a dedicated manga-pipeline
refactor. Category 4 must not be touched.
