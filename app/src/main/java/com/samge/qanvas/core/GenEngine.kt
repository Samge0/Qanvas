package com.samge.qanvas.core

import android.content.Context
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
    const val REQUIRED_BYTES: Long = 11_000_000_000L // ~10.6 GB download, ~11 GB installed
    const val PREFS_NAME = "qanvas"
    const val KEY_MODEL_DIR = "model_dir"
    const val KEY_PROXY_ENABLED = "proxy_enabled"
    const val KEY_PROXY_HOST = "proxy_host"
    const val KEY_PROXY_PORT = "proxy_port"
    const val KEY_KEEP_LOADED = "keep_models_loaded"
    const val KEY_BG_SILENT = "bg_silent_keepalive"
    const val KEY_BG_OVERLAY = "bg_progress_overlay"
    const val KEY_DL_SOURCE = "dl_source" // 0 = huggingface.co, 1 = hf-mirror.com

    fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Effective model directory: user override or the default app-private external dir. */
    fun modelDir(context: Context): File {
        val custom = prefs(context).getString(KEY_MODEL_DIR, null)
        if (!custom.isNullOrBlank()) {
            val f = File(custom)
            if (f.isDirectory && f.canWrite()) return f
        }
        return File(context.getExternalFilesDir(null), MODEL_DIR_NAME)
    }

    fun missingFiles(context: Context): String? = QwenImage21.missingFiles(modelDir(context))

    fun missingEditFiles(context: Context): String? = QwenImage21.missingEditFiles(modelDir(context))

    fun availableMemoryMB(): Int = QwenImage21.availableMemoryMB()

    fun ramOk(): Boolean = availableMemoryMB() >= 10_500

    // ---- device compatibility analysis -------------------------------------
    // Thresholds replicated from libMNN's QwenImage21Diffusion::ensureMemory
    // (verified by disassembly): a stage passes when
    //   MemAvailable >= stageNeedMB + 400 MB headroom.
    // Stage needs: text-encoder load 5600 (fixed), DiT denoise
    // 5000 + size-dependent term (Tiny ≈ 5600-5800), VAE decode
    // formula + 500. The TE gate is parameter-independent, so it is the
    // make-or-break check for low-RAM devices.

    /** Minimum MemAvailable that clears the hard text-encoder gate. */
    const val MIN_TE_MEM_MB: Int = 5600 + 400

    /** MemAvailable needed for the largest tier (safe upper bound for all stages). */
    const val RECOMMENDED_MEM_MB: Int = 10_500

    data class MemCheck(
        val availableMB: Int,
        val totalMB: Int,
        val passesHardGate: Boolean,     // >= 6000 MB: Tiny/Fast will run
        val comfortable: Boolean,        // >= 10.5 GB: all tiers comfortable
        val teNeedMB: Int = 5600,
        val headroomMB: Int = 400,
    ) {
        val headroomLeftMB: Int get() = availableMB - (teNeedMB + headroomMB)
    }

    fun memCheck(context: Context): MemCheck {
        val avail = availableMemoryMB()
        val total = runCatching {
            val mi = android.app.ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
            (mi.totalMem / (1024L * 1024L)).toInt()
        }.getOrDefault(0)
        return MemCheck(
            availableMB = avail,
            totalMB = total,
            passesHardGate = avail >= MIN_TE_MEM_MB,
            comfortable = avail >= RECOMMENDED_MEM_MB,
        )
    }

    fun freeStorageBytes(context: Context): Long =
        modelDir(context).parentFile?.usableSpace
            ?: context.getExternalFilesDir(null)?.usableSpace ?: 0L

    /** Proxy settings from prefs, or null when disabled/incomplete. */
    fun proxy(context: Context): Pair<String, Int>? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_PROXY_ENABLED, false)) return null
        val host = p.getString(KEY_PROXY_HOST, "")?.trim().orEmpty()
        val port = p.getInt(KEY_PROXY_PORT, -1)
        return if (host.isNotEmpty() && port in 1..65535) host to port else null
    }

    /** Total bytes of downloaded model files in the effective dir. */
    fun modelBytes(context: Context): Long =
        modelDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }

    /** Delete every model file; returns bytes freed. */
    fun deleteModels(context: Context): Long {
        val dir = modelDir(context)
        val freed = modelBytes(context)
        dir.deleteRecursively()
        return freed
    }

    fun makeDownloader(): ModelDownloader = ModelDownloader()

    /** Download sources: official HF, or the hf-mirror.com CN mirror (same repo,
     *  byte-identical files, URL layout identical — only the host differs). */
    enum class DlSource(val label: String, val host: String) {
        HF("huggingface.co", "https://huggingface.co/"),
        MIRROR("hf-mirror.com", "https://hf-mirror.com/"),
    }

    fun dlSource(context: Context): DlSource {
        val ord = prefs(context).getInt(KEY_DL_SOURCE, 1) // mirror default: CN-friendly
        return if (ord == 0) DlSource.HF else DlSource.MIRROR
    }

    fun setDlSource(context: Context, src: DlSource) {
        prefs(context).edit().putInt(KEY_DL_SOURCE, if (src == DlSource.HF) 0 else 1).apply()
    }

    /**
     * Progress percent → human stage label (i18n happens in the UI layer).
     * 0-10 prefix/text encoder, ~10-85 DiT steps, 85-100 VAE decode.
     */
    fun stageKind(percent: Int): String = when {
        percent <= 4 -> "load"
        percent <= 10 -> "te"
        percent < 85 -> "denoise"
        else -> "vae"
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
