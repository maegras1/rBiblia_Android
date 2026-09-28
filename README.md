# rBiblia for Android

Modern native Android application for Bible study, reading, comparison, notes, and search, rewritten from the official rBiblia web client.

## Features

- **Multi-Translation Bible Reader**: Access built-in translations repository online (e.g. King James Version, Uwspółcześniona Biblia Gdańska, Luther Bibel, and many more).
- **Navigation**: Intuitive Book selector with Old Testament, New Testament, and Deuterocanonical tabs, and quick Chapter picker.
- **Verse Comparison**: Compare any verse across all available or favorite translations with word-level difference highlighting.
- **Chapter Comparison**: Compare entire chapters side-by-side or verse-by-verse between translations.
- **Full-Text Search**: Search keywords or phrases across entire translations with scope filters (All, OT, NT) and recent search history.
- **Personal Notes**: Tap or long-press any verse to write personal study notes (stored locally with persistence), toggle global vs translation-specific notes, and export notes to XML.
- **Customizable Reading Experience**:
  - Adjustable text sizes (Small, Medium, Large, X-Large)
  - Typography options: Serif, Sans-Serif, and Monospace
  - Verse-by-verse and continuous text reading modes
  - Option to hide/show verse numbers
  - Immersive Zen Mode
- **Themes & Dark Mode**:
  - Light, Dark, and System theme
  - Two dark mode styles: Golden Warm Dark and Night Blue
- **Multi-Language UI**: Full localization support for English (EN), Polish (PL), and German (DE).
- **Translation Error Reporting**: Built-in dialog to submit typos or translation corrections directly to the rBiblia API.

## Technical Architecture

- **Platform**: Android SDK 36 (minSdk 26)
- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose & Material 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
- **Networking**: OkHttp 4.12.0 connecting to `https://rbiblia.app/api/`
- **Data Persistence**: SQLite (local database for notes, bookmarks, searches, and reading progress) and SharedPreferences
- **Adaptive Icons**: Custom adaptive launcher icon with Material You support

## Original Project & Credits

Created by Rafał Toborek with cooperation of Brothers in Faith.
Official site: [https://rbiblia.app](https://rbiblia.app)

*Jezus żyje! ✝️*
