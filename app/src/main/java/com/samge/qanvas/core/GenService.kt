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
 * Foreground service for generation (up to ~8 min) and model download.
 * Timing breakdown: [startAt] → model load → [genMs] (first denoise → done) → [endAt].
 * Optional hot reload keeps the QwenImage21 instance alive between jobs (Settings toggle).
 */
class GenService : Service() {

    companion object {
        const val CH_GEN = "qanvas_gen"
        const val CH_DL = "qanvas_download"
        const val NOTIF_GEN = 41
        const val NOTIF_DL = 42
        const val NOTIF_REQ_GEN = 1041
        const val NOTIF_REQ_DL = 1042

        const val ACTION_GENERATE = "com.samge.qanvas.GENERATE"
        const val ACTION_DOWNLOAD = "com.samge.qanvas.DOWNLOAD"
        const val ACTION_STOP = "com.samge.qanvas.STOP"
        const val ACTION_RELEASE = "com.samge.qanvas.RELEASE"

        const val X_PROMPT = "prompt"
        const val X_MODE = "mode"
        const val X_RATIO = "ratio"
        const val X_TIER = "tier"
        const val X_STEPS = "steps"
        const val X_SEED = "seed"
        const val X_INPUT = "input"
        const val X_OUT = "out"

        /** Cooperative cancellation flag for the running generation. */
        @Volatile
        var cancelRequested: Boolean = false
            private set

        fun requestCancel() {
            cancelRequested = true
        }

        @Volatile
        var running: Boolean = false
            private set

        /** Hot-reload singleton: reused across jobs when keepModelsLoaded is on. */
        @Volatile
        private var hotInstance: QwenImage21? = null
        private val hotLock = Any()

        fun releaseHot() {
            synchronized(hotLock) {
                hotInstance?.let { runCatching { it.close() } }
                hotInstance = null
            }
        }

        fun hotActive(): Boolean = hotInstance != null

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
        listOf(CH_GEN to getString(R.string.notif_channel_gen), CH_DL to getString(R.string.notif_channel_dl)).forEach { (id, name) ->
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
            ACTION_RELEASE -> {
                releaseHot()
                stopSelf()
            }
            ACTION_DOWNLOAD -> {
                startForeground(NOTIF_DL, notif(CH_DL, getString(R.string.notif_downloading), "", 0, true, NOTIF_REQ_DL))
                GenBus.post(GenBus.State(GenBus.Kind.DOWNLOADING, 0, detail = "…"))
                runDownload()
            }
            ACTION_GENERATE -> {
                val out = intent.getStringExtra(X_OUT) ?: return START_NOT_STICKY
                // immediate foreground promotion: visible notification + boosted oom_adj
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    startForeground(
                        NOTIF_GEN,
                        notif(CH_GEN, getString(R.string.notif_generating), getString(R.string.notif_preparing), 0, true, NOTIF_REQ_GEN),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                } else {
                    startForeground(
                        NOTIF_GEN,
                        notif(CH_GEN, getString(R.string.notif_generating), getString(R.string.notif_preparing), 0, true, NOTIF_REQ_GEN),
                    )
                }
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
            val wl = androidx.core.content.ContextCompat.getSystemService(
                this@GenService, android.os.PowerManager::class.java
            )!!.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "qanvas:dl")
            wl.acquire(4 * 60 * 60 * 1000L)
            try {
                val proxy = GenEngine.proxy(this@GenService)
                if (proxy != null) {
                    System.setProperty("proxyHost", proxy.first)
                    System.setProperty("proxyPort", proxy.second.toString())
                    System.setProperty("https.proxyHost", proxy.first)
                    System.setProperty("https.proxyPort", proxy.second.toString())
                }
                val d = ModelDownloader()
                downloader = d
                GenBus.post(GenBus.State(GenBus.Kind.DOWNLOADING, 0, detail = "__preparing__"))
                d.download(GenEngine.modelDir(this@GenService)) { file, done, total ->
                    val pct = (100L * done / total.coerceAtLeast(1L)).toInt()
                    val detail = String.format("%.2f/%.2f GB", done / 1e9, total / 1e9)
                    notifyProgress(NOTIF_DL, CH_DL, "dl", detail, pct)
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
                runCatching { if (wl.isHeld) wl.release() }
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
        cancelRequested = false
        val prefs = GenEngine.prefs(this)
        val useSilent = prefs.getBoolean(GenEngine.KEY_BG_SILENT, true)
        val useOverlay = prefs.getBoolean(GenEngine.KEY_BG_OVERLAY, true)
        // Partial wakelock: keeps the CPU/GPU pipeline alive with screen off.
        val wl = androidx.core.content.ContextCompat.getSystemService(
            this, android.os.PowerManager::class.java
        )!!.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "qanvas:gen")
        wl.acquire(15 * 60 * 1000L)
        // Media-process class: OEM freezers never freeze an app that is "playing audio".
        if (useSilent) BgKeepAlive.startSilent(this)
        if (useOverlay) BgKeepAlive.showOverlay(this@GenService, getString(R.string.overlay_text_fmt, 0))
        job = scope.launch {
            // run the whole coroutine on Default but boost its worker thread
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)
            var doneFile: File? = null
            var error: String? = null
            var oom = false
            var cancelledByUser = false
            var width = 0
            var height = 0
            var pausedMs = 0L
            var lastProgressAt = android.os.SystemClock.elapsedRealtime()
            val startAt = System.currentTimeMillis()
            val perf = android.os.SystemClock.elapsedRealtime()
            var modelLoadEnd = perf
            val firstDenoiseAt = longArrayOf(0L)
            try {
                val size = QwenImage21SizeProxy.of(ratioOrd, tierOrd)
                width = size.width
                height = size.height
                val keepLoaded = GenEngine.prefs(this@GenService)
                    .getBoolean(GenEngine.KEY_KEEP_LOADED, false)
                val opts = QwenImage21.Options().apply {
                    useGpu = true
                    textEncoderOnCpu = true
                    vaeOnCpu = true
                    keepModelsLoaded = keepLoaded
                    threads = 4
                    crashMarkerFile = File(filesDir, "generation_in_progress.txt")
                }
                GenBus.post(GenBus.State(GenBus.Kind.LOADING, stage = "load"))

                // hot path: reuse the loaded instance; cold path: create (and keep or close)
                val qi: QwenImage21 = synchronized(hotLock) {
                    val existing = hotInstance
                    if (keepLoaded && existing != null) existing
                    else {
                        val created = QwenImage21(GenEngine.modelDir(this@GenService), opts)
                        if (keepLoaded) hotInstance = created
                        created
                    }
                }
                modelLoadEnd = android.os.SystemClock.elapsedRealtime()

                try {
                    val listener = QwenImage21.ProgressListener { p ->
                        if (cancelRequested) {
                            throw QwenImage21Exception(QwenImage21Exception.RUNTIME_ERROR, "cancelled by user")
                        }
                        // stall telemetry: native steps land every few seconds; any gap
                        // beyond 40 s means the process was frozen/throttled meanwhile.
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastProgressAt > 40_000L) {
                            pausedMs += now - lastProgressAt
                        }
                        lastProgressAt = now
                        if (useOverlay) BgKeepAlive.updateOverlay(this@GenService, getString(R.string.overlay_text_fmt, p))
                        if (firstDenoiseAt[0] == 0L && p > 10) {
                            firstDenoiseAt[0] = android.os.SystemClock.elapsedRealtime()
                        }
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
                } finally {
                    if (!keepLoaded) {
                        runCatching { qi.close() }
                    }
                }
            } catch (e: QwenImage21Exception) {
                if (cancelRequested) {
                    cancelledByUser = true
                } else {
                    oom = e.isOutOfMemory
                    error = e.message
                }
                releaseHot()
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
                releaseHot()
            } finally {
                val endMs = android.os.SystemClock.elapsedRealtime()
                val endAt = System.currentTimeMillis()
                val genStart = if (firstDenoiseAt[0] > 0) firstDenoiseAt[0] else modelLoadEnd
                val loadMs = genStart - perf
                val genMs = endMs - genStart
                val dur = endMs - perf
                if (doneFile != null) {
                    GenBus.post(GenBus.State(GenBus.Kind.DONE, 100, "done",
                        doneFile = doneFile!!.absolutePath, pausedMs = pausedMs))
                } else if (cancelledByUser) {
                    GenBus.post(GenBus.State(GenBus.Kind.ERROR, error = "__cancelled__"))
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
                                startAt = startAt, modelLoadMs = loadMs, genMs = genMs, pausedMs = pausedMs,
                                endAt = endAt, ratio = 0, tier = 0,
                            )
                        )
                    }
                } catch (_: Exception) {
                }
                running = false
                BgKeepAlive.stopSilent()
                BgKeepAlive.removeOverlay(this@GenService)
                runCatching { if (wl.isHeld) wl.release() }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    // ---------------------------------------------------------------- notifications

    /** Notification tap → open the app (routes to the relevant tab via extras). */
    private fun contentIntent(req: Int): PendingIntent = PendingIntent.getActivity(
        this, req,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", if (req == NOTIF_REQ_DL) 5 else 0)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notif(channel: String, text: String, sub: String, pct: Int, indeterminate: Boolean, req: Int): Notification =
        NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSubText(sub)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(contentIntent(req))
            .setProgress(100, pct, indeterminate)
            .build()

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
                .setContentIntent(contentIntent(if (id == NOTIF_DL) NOTIF_REQ_DL else NOTIF_REQ_GEN))
                .setProgress(100, pct, false)
                .build(),
        )
    }
}
