# Qanvas · AI 画布 in your pocket

**Qwen-Image-2.1 (7B) text-to-image, photo editing and RGBA stickers — 100% on-device, 100% private.**

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

## License

- App code: Apache-2.0
- Model weights: Qwen Research License (research / non-commercial use) — see the
  [model card](https://huggingface.co/evankuo/Qwen-Image-2.1-MNN). This app does not commercialize
  model outputs.
