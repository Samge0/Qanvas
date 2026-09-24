package com.samge.qanvas.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.samge.qanvas.MainActivity
import com.samge.qanvas.QanvasApp
import com.samge.qanvas.R
import com.samge.qanvas.data.GenRecord
import com.scsonic.qwenimage21.ModelDownloader
import com.scsonic.qwenimage21.QwenImage21
import com.scsonic.qwenimage21.QwenImage21Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service that keeps one generation (up to ~8 min) or one model download alive.
 * The blocking QwenImage21 calls run on Dispatchers.Default; progress is published to [GenBus].
 */
class GenService : Service() {

    companion object {
        const val CH_GEN = "qanvas_gen"
        const val CH_DL = "qanvas_download"
        const val NOTIF_GEN = 41
        const val NOTIF_DL = 42

        const val ACTION_GENERATE = "com.samge.qanvas.GENERATE"
        const val ACTION_DOWNLOAD = "com.samge.qanvas.DOWNLOAD"
        const val ACTION_STOP = "com.samge.qanvas.STOP"

        const val X_PROMPT = "prompt"
        const val X_MODE = "mode"        // t2i | edit | sticker
        const val X_RATIO = "ratio"      // QwenImage21.Size.Ratio ordinal
        const val X_TIER = "tier"        // QwenImage21.Size.Tier ordinal
        const val X_STEPS = "steps"
        const val X_SEED = "seed"
        const val X_INPUT = "input"      // input image path (edit mode)
        const val X_OUT = "out"

        /** Live flag for UI gating. */
        @Volatile
        var running: Boolean = false
            private set

        fun startGeneration(
            context: Context, prompt: String, mode: String,
            ratio: Int, tier: Int, steps: Int, seed: Long,
            input: String?, out: String,
        ) {
            val i = Intent(context, GenService::class.java).apply {
                action = ACTION_GENERATE
                putExtra(X_PROMPT, prompt)
                putExtra(X_MODE, mode)
                putExtra(X_RATIO, ratio)
                putExtra(X_TIER, tier)
                putExtra(X_STEPS, steps)
                putExtra(X_SEED, seed)
                if (input != null) putExtra(X_INPUT, input)
                putExtra(X_OUT, out)
            }
            ContextCompat.startForegroundService(context, i)
        }

        fun startDownload(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, GenService::class.java).apply { action = ACTION_DOWNLOAD },
            )
        }

        fun requestStop(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, GenService::class.java).apply { action = ACTION_STOP },
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val cancelled = AtomicBoolean(false)
    private var downloader: ModelDownloader? = null
    private lateinit var notifMgr: NotificationManager

    // ---------------------------------------------------------------- lifecycle

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifMgr = getSystemService(NotificationManager::class.java)
        listOf(CH_GEN to "Generation", CH_DL to "Model download").forEach { (id, name) ->
            notifMgr.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                cancelled.set(true)
                downloader?.cancel()
                job?.cancel()
                stopSelf()
            }
            ACTION_DOWNLOAD -> {
                startForeground(NOTIF_DL, notif(CH_DL, getString(R.string.notif_downloading), "", 0, true))
                GenBus.post(GenBus.State(GenBus.Kind.DOWNLOADING, 0, detail = "…"))
                runDownload()
            }
            ACTION_GENERATE -> {
                val out = intent.getStringExtra(X_OUT) ?: return START_NOT_STICKY
                startForeground(NOTIF_GEN, notif(CH_GEN, "Preparing", "", 0, true))
                runGeneration(
                    prompt = intent.getStringExtra(X_PROMPT) ?: "",
                    mode = intent.getStringExtra(X_MODE) ?: "t2i",
                    ratioOrd = intent.getIntExtra(X_RATIO, 0),
                    tierOrd = intent.getIntExtra(X_TIER, 0),
                    steps = intent.getIntExtra(X_STEPS, 20),
                    seed = intent.getLongExtra(X_SEED, 42),
                    inputPath = intent.getStringExtra(X_INPUT),
                    out = File(out),
                )
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- download

    private fun runDownload() {
        if (running) return
        running = true
        job = scope.launch {
            try {
                // user-configurable HTTP proxy (Settings), default = system resolver
                val proxy = GenEngine.proxy(this@GenService)
                if (proxy != null) {
                    System.setProperty("proxyHost", proxy.first)
                    System.setProperty("proxyPort", proxy.second.toString())
                    System.setProperty("https.proxyHost", proxy.first)
                    System.setProperty("https.proxyPort", proxy.second.toString())
                }
                val d = ModelDownloader()
                downloader = d
                // metadata (HEAD requests for 20 files) can take seconds — show a
                // "preparing" state immediately so the UI isn't dead.
                GenBus.post(GenBus.State(GenBus.Kind.DOWNLOADING, 0, detail = "__preparing__"))
                d.download(GenEngine.modelDir(this@GenService)) { file, done, total ->
                    val pct = (100L * done / total.coerceAtLeast(1L)).toInt()
                    val detail = String.format("%.2f/%.2f GB", done / 1e9, total / 1e9)
                    notifyProgress(NOTIF_DL, CH_DL, file, detail, pct)
                    GenBus.post(GenBus.State(GenBus.Kind.DOWNLOADING, pct, file, detail = detail))
                }
                GenBus.post(GenBus.State(GenBus.Kind.DL_OK))
            } catch (e: Exception) {
                if (cancelled.get()) {
                    GenBus.post(GenBus.State(GenBus.Kind.DL_ERROR, error = "cancelled"))
                } else {
                    GenBus.post(GenBus.State(GenBus.Kind.DL_ERROR, error = e.message ?: "download failed"))
                }
            } finally {
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    // ---------------------------------------------------------------- generation

    private fun runGeneration(
        prompt: String, mode: String, ratioOrd: Int, tierOrd: Int,
        steps: Int, seed: Long, inputPath: String?, out: File,
    ) {
        if (running) {
            GenBus.post(GenBus.State(GenBus.Kind.ERROR, error = "another job is running"))
            stopSelf()
            return
        }
        running = true
        job = scope.launch {
            var doneFile: File? = null
            var error: String? = null
            var oom = false
            var width = 0
            var height = 0
            val start = android.os.SystemClock.elapsedRealtime()
            try {
                val size = QwenImage21SizeProxy.of(ratioOrd, tierOrd)
                width = size.width
                height = size.height
                val opts = QwenImage21.Options().apply {
                    useGpu = true
                    textEncoderOnCpu = true
                    vaeOnCpu = true
                    keepModelsLoaded = false
                    threads = 4
                    crashMarkerFile = File(filesDir, "generation_in_progress.txt")
                }
                GenBus.post(GenBus.State(GenBus.Kind.LOADING, stage = "load"))
                QwenImage21(GenEngine.modelDir(this@GenService), opts).use { qi ->
                val listener = QwenImage21.ProgressListener { p ->
                    notifyProgress(NOTIF_GEN, CH_GEN, GenEngine.stageKind(p), "$p%", p)
                    GenBus.post(
                        GenBus.State(
                            GenBus.Kind.GENERATING, p,
                            stageKey = GenEngine.stageKind(p),
                            stageStep = ((p - 10).coerceAtLeast(0) / 75.0 * steps).toInt().coerceAtMost(steps),
                        )
                    )
                }
                    when (mode) {
                        "edit" -> {
                            val tiers = QwenImage21SizeProxy.TIERS
                            val tier = tiers[tierOrd.coerceIn(0, tiers.size - 1)]
                            val inF = inputPath?.let { File(it) }
                            val edited = qi.edit(prompt, inF, tier, steps, seed.toInt(), out, listener)
                            width = edited?.width ?: width
                            height = edited?.height ?: height
                        }
                        else -> qi.generate(prompt, size, steps, seed.toInt(), out, listener)
                    }
                    doneFile = out
                }
            } catch (e: QwenImage21Exception) {
                oom = e.isOutOfMemory
                error = e.message
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                val dur = android.os.SystemClock.elapsedRealtime() - start
                if (doneFile != null) {
                    GenBus.post(GenBus.State(GenBus.Kind.DONE, 100, "Done", doneFile = doneFile!!.absolutePath))
                } else {
                    GenBus.post(
                        GenBus.State(if (oom) GenBus.Kind.OOM else GenBus.Kind.ERROR, error = error ?: "unknown")
                    )
                }
                try {
                    val dao = (application as QanvasApp).db.dao()
                    withContext(Dispatchers.IO) {
                        dao.insert(
                            GenRecord(
                                prompt = prompt, mode = mode, width = width, height = height,
                                steps = steps, seed = seed, outPath = out.absolutePath,
                                inPath = inputPath, durationMs = dur,
                            )
                        )
                    }
                } catch (_: Exception) {
                }
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    // ---------------------------------------------------------------- notifications

    private fun notif(channel: String, text: String, sub: String, pct: Int, indeterminate: Boolean): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSubText(sub)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pi)
            .setProgress(100, pct, indeterminate)
            .build()
    }

    private fun notifyProgress(id: Int, channel: String, stageKey: String, sub: String, pct: Int) {
        val text = when (stageKey) {
            "dl" -> getString(R.string.notif_downloading)
            "load" -> getString(R.string.notif_stage_load)
            "te" -> getString(R.string.notif_stage_te)
            "vae" -> getString(R.string.notif_stage_vae)
            else -> getString(R.string.notif_generating)
        }
        notifMgr.notify(
            id,
            NotificationCompat.Builder(this, channel)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(text)
                .setSubText(sub)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setProgress(100, pct, false)
                .build(),
        )
    }
}
