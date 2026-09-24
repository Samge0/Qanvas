package com.samge.qanvas.ui

import android.app.Application
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
import java.util.Locale

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

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as QanvasApp).db
    private val prefs = app.getSharedPreferences("qanvas", Application.MODE_PRIVATE)

    // ---- gate ----
    private val _gate = MutableStateFlow(GateStatus())
    val gate: StateFlow<GateStatus> = _gate.asStateFlow()

    // ---- bus ----
    val gen: StateFlow<GenBus.State> = GenBus.state

    // ---- history ----
    val history: StateFlow<List<GenRecord>> = db.dao().recent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), emptyList())

    // ---- last result bitmap ----
    private val _result = MutableStateFlow<Bitmap?>(null)
    val result: StateFlow<Bitmap?> = _result.asStateFlow()

    // ---- edit input ----
    private val _editInput = MutableStateFlow<PickedImage?>(null)
    val editInput: StateFlow<PickedImage?> = _editInput.asStateFlow()

    // ---- toast bus ----
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun toast(key: String) { _toast.value = key }
    fun toastShown() { _toast.value = null }

    // ---- persisted UI prefs ----
    val ratioOrdinal = MutableStateFlow(prefs.getInt("ratio", 0))
    val tierOrdinal = MutableStateFlow(prefs.getInt("tier", 1))   // Fast default: better identity keep
    val steps = MutableStateFlow(prefs.getInt("steps", 20))
    val seedText = MutableStateFlow(prefs.getString("seed", "42") ?: "42")

    /** 0 = system, 1 = en, 2 = zh */
    val language = MutableStateFlow(prefs.getInt("language", 0))

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
        // recreate so all composables re-resolve stringResource()
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

    /** Observe bus DONE → decode bitmap. */
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

    fun clearEditImage() {
        _editInput.value = null
    }

    fun deleteRecord(rec: GenRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            db.dao().delete(rec.id)
            rec.outPath.let { File(it).delete() }
        }
    }
}
