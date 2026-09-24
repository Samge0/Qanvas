package com.samge.qanvas.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-process event bus between [GenService] and the UI.
 * Simpler and more reliable than broadcast receivers for app-internal state.
 */
object GenBus {

    enum class Kind { IDLE, DOWNLOADING, LOADING, GENERATING, DONE, OOM, ERROR, DL_OK, DL_ERROR }

    data class State(
        val kind: Kind = Kind.IDLE,
        /** 0..100 (generation progress or overall download progress) */
        val progress: Int = 0,
        /** stage key for i18n: "" | "load" | "te" | "denoise" | "vae" | filename during download */
        val stage: String = "",
        /** output file when kind == DONE */
        val doneFile: String? = null,
        /** message when kind == ERROR / DL_ERROR */
        val error: String? = null,
        /** detail line for downloads: "3.21/10.28 GB · dit.mnn.weight" */
        val detail: String = "",
        /** denoising step derived from progress (UI renders "7/20" with its own strings) */
        val stageKey: String = "",
        val stageStep: Int = 0,
        /** total ms the process appeared frozen/throttled during this job */
        val pausedMs: Long = 0,
        /** tab that initiated this job (0=create, 1=sticker, 2=edit); -1 = none */
        val originTab: Int = -1,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun post(s: State) {
        _state.value = s
    }

    fun reset() {
        _state.value = State()
    }
}
