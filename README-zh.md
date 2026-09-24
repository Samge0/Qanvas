# Qanvas · 口袋里的 AI 画布

[English](README.md) | 简体中文

> 🌐 **[在线宣传页](https://samge0.github.io/Qanvas/)** — 功能总览、后台生成原理、真机性能数据一页看懂

**Qwen-Image-2.1 (7B) 文生图、图片编辑与 RGBA 透明贴纸 — 100% 端侧运行，100% 隐私。**

[![Release](https://img.shields.io/github/v/release/Samge0/Qanvas?label=Release)](../../releases)
[![License](https://img.shields.io/badge/code-Apache--2.0-blue)](LICENSE)
[![Model](https://img.shields.io/badge/model-Qwen--Image--2.1--MNN-6C5CE7)](https://huggingface.co/evankuo/Qwen-Image-2.1-MNN)

> ⬇️ **下载**：[`Qanvas-v1.x.x-arm64.apk`](../../releases)（应用本体 20 MB · 模型 10.3 GB 首次在应用内一次性下载）

无云端。无账号。无上传。你的照片永远不会离开手机。

基于 [evankuo/Qwen-Image-2.1-MNN](https://huggingface.co/evankuo/Qwen-Image-2.1-MNN) 与
[scsonic/libQwenImage21](https://github.com/scsonic/libQwenImage21) 运行时（MNN · int4 · OpenCL）构建。


> 演示视频经过压缩，下载 APK 可获得最佳画质。



https://github.com/user-attachments/assets/901ceba3-d1f2-41c7-8134-39de774b952b



## 功能一览

| Tab | 能力 |
|---|---|
| **创作** | 文生图，7 种画幅 × 3 档画质，步数/种子可控，实时分阶段进度 |
| **贴纸** | 原生 **RGBA 透明** PNG 贴纸（Qwen-Image-2.1 端侧独有能力），棋盘格透明预览 |
| **编辑** | 保持人物一致性的图片编辑 — 默认 Fast 档（上游实测其人脸保真优于 Standard） |
| **画廊** | 每次生成的完整参数记录，一键删除 |
| **灵感** | 围绕模型四大旗舰能力精选的提示词库 |

长任务运行在**前台服务**中 — 一次 7.5 分钟的生成可以扛过熄屏与应用切换，通知栏实时显示阶段进度（"文本编码 → 去噪 7/20 → VAE 解码"）。

## 后台生成与激进 ROM

一张全尺寸图需要 3–9 分钟。Qanvas 在前台服务中运行，并叠加了全部标准保活手段，切走应用后依然继续：

- **Partial 唤醒锁**全程持有（熄屏后 CPU 不睡）
- **静音音频保活** — 一个静音循环播放器把进程划入"媒体播放"类，任何 OEM 冻结器（MIUI/HyperOS、ColorOS、One UI……）都不会挂起它
- **悬浮进度胶囊** — 一个小悬浮窗（"Qanvas · 37%"）保住可见窗口，GPU 调度器就不会饿死我们的 OpenCL 命令流；同时你能在任何应用上方看到实时进度
- **电池优化豁免** + 悬浮窗权限，均在 *设置 → 后台运行* 一键授予

如果你的 ROM 上进度依然会停（完成时会提示"后台停顿 N 秒"）：

1. 在 *设置 → 后台运行* 授予那两项权限
2. 在最近任务里锁定 Qanvas（下拉卡片 → 锁定 🔒）
3. 在 *设置 → 电池 → 启动管理* 里允许自启动（关闭"自动管理"）
4. **最稳方案**：如果 ROM 支持，把 Qanvas 放进**自由窗口 / 悬浮窗**运行（多数 MIUI/HyperOS、ColorOS 与 HarmonyOS 机型都支持 — 最近任务 → 应用卡片 → 悬浮窗图标）。悬浮窗让应用始终可见地待在屏幕一角，生成全程满速、零冻结 — 连进度胶囊都不需要了。

## 环境要求

- arm64 Android 8.0+（API 26），带 OpenCL GPU
- **建议 12 GB+ 内存**（首次启动会显示内存/存储门槛卡片）
- 约 11 GB 可用存储，用于模型一次性下载（约 10.3 GB，支持断点续传，校验和验证）

## 性能（上游实测，骁龙 8 Gen 2）

| 20 步 | Standard | Fast | Tiny |
|---|---|---|---|
| 文生图 | 451 s (448×576) | 289 s (512×288) | 217 s (320×320) |
| 图片编辑 | 556 s (448×576) | 348 s (352×448) | — |

全新安装后的第一张图：选 **Tiny + 12 步**，约 2 分钟即可看到结果。

## 安装

从 [Releases](../../releases) 下载 `Qanvas-v*-arm64.apk`，或自行构建：

```bash
./gradlew assembleDebug
```

## 技术说明

- 内置官方运行时 AAR（v0.2.2，预编译 `libMNN.so` — 2026-09-23 重新导出的模型必须配这代运行时；不要把旧 AAR 与新权重混用）。
- Room 存历史记录，Compose + Material 3（Apple 风格设计令牌），StateFlow 事件总线，WinSW 级别的前台服务纪律。
- 产品与增长文档：[docs/PRD.md](docs/PRD.md) · [docs/GROWTH.md](docs/GROWTH.md)

## 签名证书

Release 构建使用专用 Qanvas 密钥签名（`CN=Qanvas, O=Samge`）：

- SHA-1: `68:45:A8:27:E9:80:CA:D8:1C:18:E6:91:53:05:F6:9D:30:C1:2E:52`
- SHA-256: `80:97:78:A8:C0:58:9C:C4:06:B2:D5:75:8A:84:91:9C:5D:EA:3A:60:3B:3C:8E:A8:95:F1:D5:1D:78:6A:DE:7F`

接入需要应用签名校验的 API 产品时，请绑定以上指纹。

## 许可

- 应用代码：Apache-2.0
- 模型权重：Qwen Research License（研究/非商业使用）— 见[模型卡](https://huggingface.co/evankuo/Qwen-Image-2.1-MNN)。本应用不对模型输出做商业化。
