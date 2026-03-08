# KanjiLens — Project Plan

## Overview
Android app for the Ayn Thor dual-screen device. Captures Japanese text from the game on the top screen, analyzes it offline, and shows an educational breakdown on the bottom screen.

**Target language**: Japanese → English
**Primary mode**: Offline-first
**Target device**: Ayn Thor (dual-screen Android)

## Core Flow
```
[Button press] → [Screenshot] → [OCR] → [Tokenize] → [Dictionary lookup] → [Result]
```

## Output on Screen
```
┌─────────────────────────────────┐
│ 今回くらい、ピエールも家族と     │  ← Original recognized text
│ すごしてくれたらいいのに…        │
├─────────────────────────────────┤
│ "I wish Pierre would spend     │  ← Full translation (online only)
│  time with the family..."      │
├─────────────────────────────────┤
│ 今回  いまかい  this time  N3   │  ← Word-by-word breakdown
│ 家族  かぞく   family     N4   │    (always available offline)
│ すごす         to spend   N3   │
│ くれる         to do for  N4   │
└─────────────────────────────────┘
```

## Tech Stack

| Layer | Tech | Size | Cost |
|-------|------|------|------|
| UI | Jetpack Compose | — | $0 |
| Capture | MediaProjection API | — | $0 |
| OCR | ML Kit Text Recognition v2 (Japanese) | ~15MB model | $0 |
| Tokenizer | Kuromoji (morphological analyzer) | ~20MB | $0 |
| Dictionary | JMDict (SQLite local) | ~15MB | $0 |
| Translation | DeepL API Free (optional, online only) | — | $0 |

**Total app size: ~50MB**

## Implementation Phases

### Phase 1 — Skeleton
- Setup Android project (Kotlin + Compose)
- Folder structure
- build.gradle with all dependencies
- MainActivity + basic navigation
- UI placeholder: button + empty result area
- **Deliverable**: app that compiles and shows screen with button

### Phase 2 — Screen Capture
- Implement ScreenCaptureManager with MediaProjection API
- Request user permissions
- Foreground Service with notification
- Capture screenshot and convert to Bitmap
- Crop to upper screen region
- **Deliverable**: app that captures screenshot and displays it

### Phase 3 — OCR
- Integrate ML Kit Text Recognition v2
- Download Japanese model on-device
- Process Bitmap → extracted Japanese text
- Show recognized text in UI
- Handle "no text found" case
- **Deliverable**: app that captures screenshot and extracts Japanese text

### Phase 4 — Tokenizer + Dictionary
- Integrate Kuromoji for morphological analysis
- Prepare JMDict as SQLite database (pre-built in assets)
- Tokenize text → word list with readings
- Lookup each word in JMDict → English meaning + JLPT level
- Create data classes: WordEntry(kanji, reading, meaning, jlptLevel)
- **Deliverable**: complete offline pipeline working

### Phase 5 — Polished UI
- Design result layout with Compose:
  - Full original text on top
  - Word breakdown list
  - Each word: kanji | reading | meaning | JLPT badge
- Dark theme (default, for gaming)
- Loading state while processing
- Error states (no text found, OCR failed)
- Font size optimized for Ayn Thor bottom screen
- **Deliverable**: functional and usable MVP

### Phase 6 — DeepL (optional online)
- Integrate DeepL API Free for full sentence translation
- Only activates when internet is available
- Settings field for API key
- Show full translation above word breakdown
- Visual indicator for offline vs online mode
- **Deliverable**: complete MVP

## Project Structure
```
kanjilens/
├── app/src/main/
│   ├── java/com/kanjilens/
│   │   ├── MainActivity.kt
│   │   ├── KanjiLensApp.kt              ← Navigation + theme
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── MainScreen.kt        ← Button + results
│   │   │   │   └── SettingsScreen.kt    ← API key, config
│   │   │   ├── components/
│   │   │   │   ├── WordCard.kt          ← Single word breakdown
│   │   │   │   ├── TranslationResult.kt ← Full result view
│   │   │   │   └── CaptureButton.kt     ← Main button
│   │   │   └── theme/
│   │   │       └── Theme.kt             ← Dark theme
│   │   ├── capture/
│   │   │   ├── ScreenCaptureManager.kt  ← MediaProjection
│   │   │   └── CaptureService.kt        ← Foreground Service
│   │   ├── ocr/
│   │   │   └── TextRecognizer.kt        ← ML Kit wrapper
│   │   ├── analysis/
│   │   │   ├── JapaneseTokenizer.kt     ← Kuromoji wrapper
│   │   │   └── DictionaryLookup.kt      ← JMDict SQLite queries
│   │   ├── translate/
│   │   │   └── DeepLService.kt          ← Online translation
│   │   ├── data/
│   │   │   ├── models/
│   │   │   │   ├── WordEntry.kt         ← Analyzed word
│   │   │   │   ├── AnalysisResult.kt    ← Full result
│   │   │   │   └── CaptureState.kt      ← UI states
│   │   │   └── db/
│   │   │       └── DictionaryDatabase.kt ← Room/SQLite
│   │   └── di/
│   │       └── AppModule.kt             ← Dependency injection
│   ├── assets/
│   │   └── jmdict.db                    ← Pre-built dictionary
│   └── AndroidManifest.xml
├── build.gradle.kts
└── gradle/
```

## Android Permissions Required
- FOREGROUND_SERVICE — to run in background
- FOREGROUND_SERVICE_MEDIA_PROJECTION — for screen capture
- MEDIA_PROJECTION — user approves once per session
- INTERNET — for DeepL API calls (optional)

## Risks and Mitigation

| Risk | Probability | Mitigation |
|------|-------------|------------|
| OCR fails with pixel fonts | High | Test with real screenshots in Phase 3, explore Tesseract as alternative |
| MediaProjection can't capture game screen | Medium | Validate in Phase 2, alternative: manual screenshot from system |
| Kuromoji fails with game dialogue | Low | JMDict as fallback for direct substring lookup |
| JMDict too large | Low | Filter to common entries (~50K vs 200K+), reduce to ~8MB |

## Post-MVP (Future)
- Export to AnkiDroid
- Word history
- Learning stats (new words per session)
- Favorites / saved words
- Auto-capture every N seconds
- Capture region selector
- Spanish language support
