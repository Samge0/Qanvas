# Qanvas · AI 画布 in your pocket

**Qwen-Image-2.1 (7B) text-to-image, photo editing and RGBA stickers — 100% on-device, 100% private.**

[![Release](https://img.shields.io/github/v/release/Samge0/Qanvas?label=Release)](../../releases)
[![License](https://img.shields.io/badge/code-Apache--2.0-blue)](LICENSE)
[![Model](https://img.shields.io/badge/model-Qwen--Image--2.1--MNN-6C5CE7)](https://huggingface.co/evankuo/Qwen-Image-2.1-MNN)

> ⬇️ **Download**: [`Qanvas-v1.0.0-arm64.apk`](../../releases) (20 MB app · one-time 10.3 GB model download in-app)

No cloud. No account. No upload. Your photos never leave the phone.

Built on [evankuo/Qwen-Image-2.1-MNN](https://huggingface.co/evankuo/Qwen-Image-2.1-MNN) + the
[scsonic/libQwenImage21](https://github.com/scsonic/libQwenImage21) runtime (MNN · int4 · OpenCL).

## What it does

| Tab | Capability |
|---|---|
| **Create** | Text-to-image, 7 aspect ratios × 3 quality tiers, steps/seed control, live stage-aware progress |
| **Sticker** | Native **RGBA transparent** PNG stickers (a Qwen-Image-2.1 exclusive on-device), checkerboard preview |
| **Edit** | Photo editing that keeps identity — Fast tier default (measured upstream to preserve faces better than Standard) |
| **Gallery** | Every generation with full parameters, one-tap delete |
| **Inspo** | Curated prompt library across the model's four flagship abilities |

Long jobs run in a **foreground service** — a 7.5-minute generation survives screen-off and app
switching, with stage progress in the notification ("Text encoder → Denoising 7/20 → VAE decode").

## Background generation & aggressive ROMs

A full-size generation takes 3–9 minutes. Qanvas runs it in a foreground service and applies
every standard mitigation so it keeps running when you switch apps:

- **Partial wake lock** held for the whole job (CPU stays up with screen off)
- **Silent-audio keep-alive** — a muted looping player puts the process in the "playing media"
  class, which no OEM freezer (MIUI/HyperOS, ColorOS, One UI …) suspends
- **Floating progress pill** — a small overlay ("Qanvas · 37%") keeps a visible window, so the
  GPU scheduler never starves our OpenCL command stream; you also get live progress over any app
- **Battery-optimization exemption** + overlay permission, both one tap from
  *Settings → Background running*

If progress still stalls on your ROM (you'll see the completion toast report
"stalled N s in background"):

1. Grant the two permissions in *Settings → Background running*
2. Lock Qanvas in the recents view (pull down on its card → lock 🔒)
3. Allow it under *Settings → Battery → App launch* (disable "auto manage")
4. **Best guarantee**: launch Qanvas in a **free-form / floating window** if your ROM supports
   it (most MIUI/HyperOS, ColorOS and HarmonyOS devices do — recents → app card → floating
   window icon). A floating window keeps the app visibly on screen next to whatever you're
   doing, so generation runs at full speed with zero freezing — and the progress pill isn't
   even needed.

## Requirements

- arm64 Android 8.0+ (API 26) with an OpenCL GPU
- **12 GB+ RAM recommended** (a RAM/storage gate card is shown at first launch)
- ~11 GB free storage for the one-time model download (~10.3 GB, resumable, checksum-verified)

## Performance (upstream-measured, Snapdragon 8 Gen 2)

| 20 steps | Standard | Fast | Tiny |
|---|---|---|---|
| Text-to-image | 451 s (448×576) | 289 s (512×288) | 217 s (320×320) |
| Image edit | 556 s (448×576) | 348 s (352×448) | — |

First image on a fresh install: pick **Tiny + 12 steps** for a ~2-minute first result.

## Install

Grab `Qanvas-v*-arm64.apk` from [Releases](../../releases), or build:

```bash
./gradlew assembleDebug
```

## Tech notes

- Ships the official runtime AAR (v0.2.2, prebuilt `libMNN.so` — the 2026-09-23 model re-export
  requires this runtime generation; don't mix older AARs with newer weights).
- Room for history, Compose + Material 3 (Apple-style tokens), StateFlow event bus, WinSW-grade
  foreground service discipline.
- Product & growth docs: [docs/PRD.md](docs/PRD.md) · [docs/GROWTH.md](docs/GROWTH.md)

## Signing certificate

Release builds are signed with a dedicated Qanvas key (`CN=Qanvas, O=Samge`):

- SHA-1: `68:45:A8:27:E9:80:CA:D8:1C:18:E6:91:53:05:F6:9D:30:C1:2E:52`
- SHA-256: `80:97:78:A8:C0:58:9C:C4:06:B2:D5:75:8A:84:91:9C:5D:EA:3A:60:3B:3C:8E:A8:95:F1:D5:1D:78:6A:DE:7F`

Bind these fingerprints when registering for API products that require app-signature verification.

## License

- App code: Apache-2.0
- Model weights: Qwen Research License (research / non-commercial use) — see the
  [model card](https://huggingface.co/evankuo/Qwen-Image-2.1-MNN). This app does not commercialize
  model outputs.
