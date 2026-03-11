# ThorLens

Android app for translating and understanding foreign-language game screens in real time. Built for dual-screen devices like the [Ayn Thor](https://www.ayntec.com/), where the game runs on the top screen and ThorLens runs on the bottom.

## What it does

**Translate mode** (primary) — Captures a screenshot of the top screen and translates it. Works with any language (Japanese, Chinese, Korean, etc). Three translation styles available with AI models:
- **Auto** (default): Translates and explains what to do next
- **Translate**: Just translates the text, no extra explanation
- **Explain**: Full translation with detailed guidance on how to progress

**JP Dictionary mode** — Offline Japanese word-by-word breakdown. Captures text via OCR, tokenizes it, and looks up each word in a 212K-entry dictionary (JMDict). Shows kanji, reading, meaning, and JLPT level. No internet required.

**Custom capture region** — By default the entire screen is captured. You can select a specific area (e.g. the dialogue box) by tapping "Full" in the top bar, then dragging on the screenshot to draw the region. Useful for reducing noise and improving translation accuracy.

## Supported models

| Model | Provider | Cost | Notes |
|-------|----------|------|-------|
| **Offline (ML Kit)** | Google | Free | On-device translation, no internet after first download (~30MB per language). Default model. |
| **Offline Auto** | Google | Free | Same as Offline, but captures automatically every 1s. Only re-translates when text changes. |
| **Gemini 2.5 Flash** | Google | Free tier available | AI vision model, requires API key |
| **GPT-4o mini** | OpenAI | Pay per use | AI vision model, requires API key |

Switch models from the top bar dropdown or in Settings. Each AI model stores its own API key separately.

## Output language

Translate into 9 languages: English, Spanish, Portuguese, French, German, Italian, Chinese, Korean, and Russian. Configurable in Settings.

For Offline mode, each language downloads a ~30MB model on first use. AI models support all languages without extra downloads.

## Tech stack

- **Kotlin + Jetpack Compose** — UI and app logic
- **MediaProjection API** — Screen capture (with ForegroundService for Android 14+)
- **ML Kit Translate** — On-device offline translation (~30MB per language)
- **OpenAI GPT-4o-mini / Google Gemini 2.5 Flash** — Vision APIs for AI Translate mode
- **ML Kit Text Recognition v2** — On-device Japanese OCR for Dictionary mode and offline translation
- **Kuromoji** — Japanese morphological analyzer/tokenizer
- **JMDict** — 212,478 entry offline Japanese-English dictionary
- **OkHttp** — HTTP client for API calls

## Setup

### Requirements
- Android SDK (compileSdk 35, minSdk 26)
- An API key for Translate mode (Google AI free tier recommended, or OpenAI)

### Build
```bash
# Clone
git clone <repo-url>
cd kanjilens

# Set your SDK path
echo "sdk.dir=$HOME/Android/sdk" > local.properties

# Build and install
./gradlew installDebug
```

### Configuration
1. Open ThorLens on your device
2. The default model is **Offline (ML Kit)** — works immediately, no API key needed
3. To use AI models: tap **...** (top right) → Settings → choose Gemini Flash or GPT-4o mini → paste your API key
4. Choose your output language (default: English)
5. Choose your preferred translation style for AI models (Auto/Translate/Explain)
6. Adjust text size if needed (S/M/L)

## Usage

1. Run your game on the top screen
2. Open ThorLens on the bottom screen
3. Switch between **Translate** (any language) and **JP Dictionary** (offline, Japanese only)
4. Switch models from the top bar dropdown (Offline → Offline Auto → Gemini → GPT-4o)
5. Press the button to capture and translate the top screen (or select Offline Auto for continuous translation)
6. Optionally tap **Full** in the top bar to select a custom capture region
7. Results persist when switching between modes or going to Settings

## Project structure

```
app/src/main/java/com/kanjilens/
├── MainActivity.kt              # Entry point, navigation, state management
├── capture/
│   ├── ScreenCaptureManager.kt  # MediaProjection + VirtualDisplay
│   └── ScreenCaptureService.kt  # ForegroundService for Android 14+
├── ocr/
│   └── TextRecognizer.kt        # ML Kit Japanese OCR wrapper
├── analysis/
│   ├── JapaneseTokenizer.kt     # Kuromoji tokenizer wrapper
│   └── DictionaryLookup.kt      # JMDict dictionary lookup
├── translate/
│   └── ScreenTranslator.kt      # Multi-model translator (ML Kit offline + OpenAI + Gemini)
├── data/models/
│   ├── AppSettings.kt           # SharedPreferences with StateFlow
│   └── CaptureState.kt          # State models (WordEntry, AnalysisResult, etc)
└── ui/
    ├── screens/
    │   ├── MainScreen.kt        # Main UI with mode toggle and model selector
    │   ├── SettingsScreen.kt    # Settings (model, language, style, keys, text size)
    │   ├── CropScreen.kt        # Visual capture region selector
    │   └── HelpScreen.kt        # Usage guide and API key instructions
    ├── components/
    │   ├── CaptureButton.kt     # Capture button with loading state
    │   ├── TranslationResult.kt # Dictionary word breakdown view
    │   └── WordCard.kt          # Individual word card (kanji, reading, meaning)
    └── theme/
        └── Theme.kt             # Dark theme (navy + pink)
```

## License

MIT
