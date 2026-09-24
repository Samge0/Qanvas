package com.samge.qanvas.core

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import java.io.File

/**
 * Background-running reliability kit for OEM-aggressive ROMs (MIUI/HyperOS, ColorOS, etc.):
 *
 *  1. [SilentKeepAlive] — a muted MediaPlayer holding audio focus. Processes with active
 *     audio are treated as "playing media" by every OEM freezer/battery manager and are
 *     never frozen or CPU-throttled. This is the single most effective known mitigation.
 *  2. [ProgressOverlay] — a tiny TYPE_APPLICATION_OVERLAY progress pill. Keeping a visible
 *     overlay window keeps the process in a "has visible UI" scheduling class, which keeps
 *     GPU command streams (OpenCL on Adreno) from being starved by the foreground app.
 *  3. Battery-optimization exemption (requested in Settings) + OEM guidance.
 */
object BgKeepAlive {

    /** 0.4 s of silence, looped muted. Generated at first use into cacheDir. */
    private var player: MediaPlayer? = null

    @SuppressLint("SdCardPath")
    fun startSilent(context: Context) {
        if (player != null) return
        runCatching {
            val wav = File(context.cacheDir, "qanvas_silent.wav")
            if (!wav.isFile) wav.writeBytes(makeSilentWav())
            val mp = MediaPlayer()
            mp.setDataSource(wav.absolutePath)
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.prepare()
            mp.start()
            player = mp
        }
    }

    fun stopSilent() {
        runCatching {
            player?.let { if (it.isPlaying) it.stop() }
            player?.release()
        }
        player = null
    }

    fun silentActive(): Boolean = runCatching { player?.isPlaying == true }.getOrDefault(false)

    /** Minimal valid 16-bit mono 8 kHz WAV with ~0.4 s of silence. */
    private fun makeSilentWav(): ByteArray {
        val sampleRate = 8000
        val seconds = 0.4
        val frames = (sampleRate * seconds).toInt()
        val dataLen = frames * 2
        val header = ByteArray(44)
        fun le32(off: Int, v: Int) { header[off] = (v and 0xff).toByte(); header[off+1] = ((v shr 8) and 0xff).toByte(); header[off+2] = ((v shr 16) and 0xff).toByte(); header[off+3] = ((v shr 24) and 0xff).toByte() }
        fun le16(off: Int, v: Int) { header[off] = (v and 0xff).toByte(); header[off+1] = ((v shr 8) and 0xff).toByte() }
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        le32(4, 36 + dataLen)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        le32(16, 16); le16(20, 1); le16(22, 1)
        le32(24, sampleRate); le32(28, sampleRate * 2); le16(32, 2); le16(34, 16)
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        le32(40, dataLen)
        return header + ByteArray(dataLen)
    }

    // ---------------------------------------------------------------- overlay

    /** Minimal overlay progress pill; null when permission missing/disabled. */
    fun showOverlay(context: Context, text: String) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) return
        runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            removeOverlay(context)
            val dp = context.resources.displayMetrics.density
            val tv = android.widget.TextView(context).apply {
                this.text = text
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 18 * dp
                    setColor(0xCC1D1D1F.toInt())
                }
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= 26)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (18 * dp).toInt()
            }
            wm.addView(tv, lp)
            overlayView = tv
        }
    }

    fun updateOverlay(context: Context, text: String) {
        // simplest robust path: re-add (cheap, once per percent)
        showOverlay(context, text)
    }

    fun removeOverlay(context: Context) {
        runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val v = overlayView
            if (v != null) wm.removeView(v)
        }
        overlayView = null
    }

    @Volatile
    private var overlayView: android.view.View? = null

    // ---------------------------------------------------------------- battery

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Fire-and-forget intent to the battery-optimization exemption dialog. */
    fun requestBatteryExemption(context: Context) {
        runCatching {
            @SuppressLint("BatteryLife")
            val i = android.content.Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:" + context.packageName),
            )
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    fun canDrawOverlays(context: Context): Boolean =
        Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

    fun requestOverlayPermission(context: Context) {
        runCatching {
            val i = android.content.Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + context.packageName),
            )
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }
}
