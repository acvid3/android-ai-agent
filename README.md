# Android AI Automation Agent

Universal Android application that can observe the screen, understand UI state, and execute actions automatically using AI-driven decision engine.

## Features

- **Screen Capture**: Uses MediaProjection API for real-time screen monitoring
- **Accessibility Integration**: Leverages AccessibilityService for UI interaction
- **AI Decision Engine**: Supports multiple AI providers (OpenAI, Claude, local models)
- **Vision Analysis**: OCR, template matching, and UI element detection
- **Routing System**: Manual route definitions for structured navigation
- **Overlay UI**: Floating button with settings panel
- **APK Distribution**: Local network APK distribution with QR code

## Architecture

### Core Modules

- `overlay/` - Floating overlay UI and settings panel
- `accessibility/` - AccessibilityService for click/swipe/input
- `screencapture/` - MediaProjection-based screen capture
- `vision/` - UI element detection and analysis
- `ocr/` - Text extraction using ML Kit
- `routing/` - Route definitions and screen identification
- `ai/` - AI provider abstraction and decision engine
- `action/` - Action execution with safety checks
- `config/` - Configuration management
- `prompt/` - AI prompt construction
- `state/` - Agent state and memory management

### APK Distribution

- `apk_distribution/local_server/` - FastAPI HTTP server
- `apk_distribution/qr_generator/` - QR code generation
- `apk_distribution/network_detector/` - Local IP detection
- `apk_distribution/apk_registry/` - APK version management

## Build

```bash
./gradlew assembleDebug
```

## APK Distribution

Start the local server:

```bash
cd apk_distribution/local_server
python server.py
```

The APK will be available at `http://<local-ip>:8000/download/app.apk`

## Permissions Required

- Accessibility Service
- Overlay Permission
- Foreground Service
- Media Projection

## Usage

1. Install the APK
2. Grant accessibility service permission
3. Grant overlay permission
4. Launch the app
5. Configure system prompt and routes
6. Start the agent
