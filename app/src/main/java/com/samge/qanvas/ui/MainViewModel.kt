package com.samge.qanvas.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samge.qanvas.QanvasApp
import com.samge.qanvas.core.GenBus
import com.samge.qanvas.core.GenEngine
import com.samge.qanvas.core.GenService
import com.samge.qanvas.core.ShareCard
import com.samge.qanvas.data.GenDao
import com.samge.qanvas.data.GenRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class GateStatus(
    val modelPresent: Boolean = false,
    val editFilesPresent: Boolean = false,
    val ramMB: Int = 0,
    val freeBytes: Long = 0,
    val checking: Boolean = true,
) {
    val ramOk: Boolean get() = ramMB >= 10_500
    val storageOk: Boolean get() = freeBytes >= GenEngine.REQUIRED_BYTES
}

data class PickedImage(val uri: Uri, val width: Int, val height: Int, val cachePath: String)

/** Cross-tab command bus: inspo → create, detail → edit, notification tap, clipboard card. */
sealed class UiEvent {
    data class GoTab(val tab: Int) : UiEvent()
    data class ApplyPrompt(val prompt: String) : UiEvent()
    data class ApplyClipboardCard(val rec: GenRecord) : UiEvent()
    data class EditImage(val record: GenRecord) : UiEvent()
    data class ShowRecord(val record: GenRecord) : UiEvent()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as QanvasApp).db
    val dao: GenDao = db.dao()
    private val prefs = app.getSharedPreferences("qanvas", Application.MODE_PRIVATE)

    private val _gate = MutableStateFlow(GateStatus())
    val gate: StateFlow<GateStatus> = _gate.asStateFlow()

    val gen: StateFlow<GenBus.State> = GenBus.state

    val history: StateFlow<List<GenRecord>> = db.dao().recent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), emptyList())

    private val _result = MutableStateFlow<Bitmap?>(null)
    val result: StateFlow<Bitmap?> = _result.asStateFlow()

    private val _editInput = MutableStateFlow<PickedImage?>(null)
    val editInput: StateFlow<PickedImage?> = _editInput.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun toast(key: String) { _toast.value = key }
    fun toastShown() { _toast.value = null }

    private val _events = MutableStateFlow<UiEvent?>(null)
    val events: StateFlow<UiEvent?> = _events.asStateFlow()
    fun eventHandled() { _events.value = null }

    val ratioOrdinal = MutableStateFlow(prefs.getInt("ratio", 0))
    val tierOrdinal = MutableStateFlow(prefs.getInt("tier", 1))
    val steps = MutableStateFlow(prefs.getInt("steps", 20))
    val seedText = MutableStateFlow(prefs.getString("seed", "42") ?: "42")
    val language = MutableStateFlow(prefs.getInt("language", 0))

    /** Prompt loaded from Inspo / detail "reuse" — the Create tab consumes it once. */
    private val _prefillPrompt = MutableStateFlow<String?>(null)
    val prefillPrompt: StateFlow<String?> = _prefillPrompt.asStateFlow()
    fun consumePrefill() { _prefillPrompt.value = null }

    /** Image record loaded into the Edit tab. */
    private val _editFromRecord = MutableStateFlow<GenRecord?>(null)
    val editFromRecord: StateFlow<GenRecord?> = _editFromRecord.asStateFlow()
    fun consumeEditFromRecord() { _editFromRecord.value = null }

    /** Clipboard share card detected on resume. */
    private val _clipCard = MutableStateFlow<GenRecord?>(null)
    val clipCard: StateFlow<GenRecord?> = _clipCard.asStateFlow()
    fun dismissClipCard() { _clipCard.value = null }

    fun applyPersistedLocale() {
        when (prefs.getInt("language", 0)) {
            1 -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            2 -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
            else -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    fun setLanguage(which: Int) {
        language.value = which
        prefs.edit().putInt("language", which).apply()
        when (which) {
            1 -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            2 -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
            else -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    fun refreshGate() {
        viewModelScope.launch(Dispatchers.IO) {
            val g = GateStatus(
                modelPresent = GenEngine.missingFiles(getApplication()) == null,
                editFilesPresent = GenEngine.missingEditFiles(getApplication()) == null,
                ramMB = GenEngine.availableMemoryMB(),
                freeBytes = GenEngine.freeStorageBytes(getApplication()),
                checking = false,
            )
            withContext(Dispatchers.Main) { _gate.value = g }
        }
    }

    fun needModel(): Boolean {
        val missing = GenEngine.missingFiles(getApplication()) != null
        if (missing) _toast.value = "__need_download__"
        return missing
    }

    fun startDownload() = GenService.startDownload(getApplication())

    fun startGeneration(prompt: String, mode: String, input: PickedImage?) {
        val app = getApplication<Application>()
        if (GenService.running) return
        prefs.edit()
            .putInt("ratio", ratioOrdinal.value)
            .putInt("tier", tierOrdinal.value)
            .putInt("steps", steps.value)
            .putString("seed", seedText.value)
            .apply()
        val seed = seedText.value.trim().toLongOrNull() ?: (System.currentTimeMillis() % 1_000_000L)
        val out = GenEngine.newOutputFile(app)
        _result.value = null
        GenService.startGeneration(
            context = app,
            prompt = prompt,
            mode = mode,
            ratio = ratioOrdinal.value,
            tier = tierOrdinal.value,
            steps = steps.value,
            seed = seed,
            input = input?.cachePath,
            out = out.absolutePath,
        )
    }

    fun collectResult() {
        viewModelScope.launch {
            gen.collect { st ->
                if (st.kind == GenBus.Kind.DONE && st.doneFile != null) {
                    val bmp = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(st.doneFile)
                    }
                    _result.value = bmp
                    refreshGate()
                }
            }
        }
    }

    /** Check clipboard for a Qanvas share card (called on each resume). */
    fun checkClipboard() {
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(getApplication())?.toString() ?: return
        if (_clipCard.value != null) return
        val rec = ShareCard.decode(text) ?: return
        _clipCard.value = rec
    }

    /** Apply a clipboard card: set params + prompt, jump to the right tab. */
    fun applyClipCard(rec: GenRecord) {
        rec.ratio.takeIf { it in 0..6 }?.let { ratioOrdinal.value = it }
        rec.tier.takeIf { it in 0..2 }?.let { tierOrdinal.value = it }
        rec.steps.takeIf { it in 1..50 }?.let { steps.value = it }
        seedText.value = rec.seed.toString()
        _prefillPrompt.value = rec.prompt
        _clipCard.value = null
        _events.value = UiEvent.GoTab(if (rec.mode == "edit") 2 else 0)
    }

    /** Inspo card tap → fill prompt, jump to Create (or Sticker for sticker cards). */
    fun applyInspo(card: com.samge.qanvas.core.Inspo.Card) {
        val zh = com.samge.qanvas.core.Inspo.isZh()
        _prefillPrompt.value = card.promptFor(zh)
        _events.value = UiEvent.GoTab(if (card.kind == com.samge.qanvas.core.Inspo.Kind.STICKER) 1 else 0)
    }

    /** Gallery detail: reuse prompt in Create. */
    fun reusePrompt(rec: GenRecord) {
        _prefillPrompt.value = rec.prompt
        _events.value = UiEvent.GoTab(0)
    }

    /** Gallery detail: send output image to Edit tab. */
    fun sendToEdit(rec: GenRecord) {
        _editFromRecord.value = rec
        _events.value = UiEvent.GoTab(2)
    }

    fun pickEditImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val dst = File(app.cacheDir, "edit_input_" + System.currentTimeMillis() + ".png")
                app.contentResolver.openInputStream(uri)!!.use { input ->
                    dst.outputStream().use { input.copyTo(it) }
                }
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(dst.absolutePath, opts)
                val picked = PickedImage(uri, opts.outWidth, opts.outHeight, dst.absolutePath)
                withContext(Dispatchers.Main) { _editInput.value = picked }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _editInput.value = PickedImage(uri, 0, 0, "")
                }
            }
        }
    }

    /** Use a gallery record's output PNG as the edit input. */
    fun useRecordAsEditInput(rec: GenRecord) {
        val f = File(rec.outPath)
        if (f.isFile) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, opts)
            _editInput.value = PickedImage(Uri.fromFile(f), opts.outWidth, opts.outHeight, f.absolutePath)
        }
    }

    fun clearEditImage() {
        _editInput.value = null
    }

    fun deleteRecord(rec: GenRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            db.dao().delete(rec.id)
            File(rec.outPath).delete()
        }
    }
}
