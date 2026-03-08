# ThorLens

Android app for translating and understanding foreign-language game screens in real time. Built for dual-screen devices like the [Ayn Thor](https://www.ayntec.com/), where the game runs on the top screen and ThorLens runs on the bottom.

## What it does

**Translate mode** (primary) — Captures a screenshot of the top screen, sends it to an AI vision model, and returns a translation + explanation of what's happening. Works with any language (Japanese, Chinese, Korean, etc). Three styles:
- **Auto** (default): Translates and explains what to do next
- **Translate**: Just translates the text, no extra explanation
- **Explain**: Full translation with detailed guidance on how to progress

**JP Dictionary mode** — Offline Japanese word-by-word breakdown. Captures text via OCR, tokenizes it, and looks up each word in a 212K-entry dictionary (JMDict). Shows kanji, reading, meaning, and JLPT level. No internet required.

## Supported AI models

| Model | Provider | Approx. cost per capture |
|-------|----------|--------------------------|
| **GPT-4o mini** | OpenAI | ~$0.01-0.02 |
| **Gemini 2.5 Flash** | Google | Free tier available |

You can switch between models in Settings. Each model stores its own API key separately.

## Tech stack

- **Kotlin + Jetpack Compose** — UI and app logic
- **MediaProjection API** — Screen capture (with ForegroundService for Android 14+)
- **OpenAI GPT-4o-mini / Google Gemini 2.5 Flash** — Vision APIs for Translate mode
- **ML Kit Text Recognition v2** — On-device Japanese OCR for Dictionary mode
- **Kuromoji** — Japanese morphological analyzer/tokenizer
- **JMDict** — 212,478 entry offline Japanese-English dictionary
- **OkHttp** — HTTP client for API calls

## Setup

### Requirements
- Android SDK (compileSdk 35, minSdk 26)
- An API key for Translate mode (OpenAI or Google AI, depending on chosen model)

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
2. Tap **...** (top right) to open Settings
3. Choose your AI model (GPT-4o mini or Gemini Flash)
4. Paste your API key (OpenAI: platform.openai.com / Google: aistudio.google.com)
5. Choose your preferred translation style (Auto/Translate/Explain)
6. Adjust text size if needed (S/M/L)

## Usage

1. Run your game on the top screen
2. Open ThorLens on the bottom screen
3. Switch between **Translate** (AI-powered, any language) and **JP Dictionary** (offline, Japanese only)
4. Press the button to capture and analyze the top screen
5. Results persist when switching between modes or going to Settings

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
│   └── ScreenTranslator.kt      # Multi-model AI translator (OpenAI + Gemini)
├── data/models/
│   ├── AppSettings.kt           # SharedPreferences with StateFlow
│   └── CaptureState.kt          # State models (WordEntry, AnalysisResult, etc)
└── ui/
    ├── screens/
    │   ├── MainScreen.kt        # Main UI with mode toggle
    │   └── SettingsScreen.kt    # Settings (model, style, keys, text size)
    ├── components/
    │   ├── CaptureButton.kt     # Capture button with loading state
    │   ├── TranslationResult.kt # Dictionary word breakdown view
    │   └── WordCard.kt          # Individual word card (kanji, reading, meaning)
    └── theme/
        └── Theme.kt             # Dark theme (navy + pink)
```

## License

MIT
