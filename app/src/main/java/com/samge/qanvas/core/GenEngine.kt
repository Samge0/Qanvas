package com.samge.qanvas.core

import android.content.Context
import android.graphics.Bitmap
import com.scsonic.qwenimage21.ModelDownloader
import com.scsonic.qwenimage21.QwenImage21
import java.io.File

/**
 * Thin wrapper around the official Qwen-Image-2.1 MNN runtime.
 *
 * Real per-stage timings from the upstream repo (SD 8 Gen 2 / Adreno 740, 20 steps):
 *  - t2i 448×576: 451 s   - t2i 512×288 (Fast): 289 s   - t2i 320×320 (Tiny): 217 s
 *  - edit → 352×448 Fast: 348 s
 */
object GenEngine {

    const val MODEL_DIR_NAME = "qwen_image21"
    const val REQUIRED_BYTES: Long = 11_000_000_000L // ~10.3 GB download, ~11 GB installed

    fun modelDir(context: Context): File = File(context.getExternalFilesDir(null), MODEL_DIR_NAME)

    fun missingFiles(context: Context): String? = QwenImage21.missingFiles(modelDir(context))

    fun missingEditFiles(context: Context): String? = QwenImage21.missingEditFiles(modelDir(context))

    fun availableMemoryMB(): Int = QwenImage21.availableMemoryMB()

    /** True when the device offers the recommended RAM headroom (12 GB+ class). */
    fun ramOk(): Boolean = availableMemoryMB() >= 10_500

    fun freeStorageBytes(context: Context): Long =
        context.getExternalFilesDir(null)?.usableSpace ?: 0L

    fun makeDownloader(): ModelDownloader = ModelDownloader()

    /**
     * Progress percent → human stage label. The engine reports:
     *  0-10 prefix/text encoder, ~10-85 DiT steps, 85-100 VAE decode.
     */
    fun stageLabel(percent: Int, steps: Int): String = when {
        percent <= 4 -> "Loading stages"
        percent <= 10 -> "Text encoder"
        percent < 85 -> {
            val done = ((percent - 10).coerceAtLeast(0) / 75.0 * steps).toInt().coerceAtMost(steps)
            "Denoising $done/$steps"
        }
        else -> "VAE decode"
    }

    /** Estimated seconds for the given size/tier/steps (SD8Gen2-class device baseline). */
    fun estimateSeconds(tokens: Int, steps: Int): Int {
        // ~19.1 s/step at 1008 tokens (448×576); scales ~linearly with tokens
        val perStep = 19.1 * tokens / 1008.0
        return (perStep * steps + 35).toInt()
    }

    fun outputsDir(context: Context): File =
        File(context.getExternalFilesDir(null), "outputs").apply { mkdirs() }

    fun newOutputFile(context: Context): File =
        File(outputsDir(context), "qanvas_" + System.currentTimeMillis() + ".png")
}
