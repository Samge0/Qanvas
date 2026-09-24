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
        /** human stage label, e.g. "Denoising 7/20" */
        val stage: String = "",
        /** output file when kind == DONE */
        val doneFile: String? = null,
        /** message when kind == ERROR / DL_ERROR */
        val error: String? = null,
        /** detail line for downloads: "3.21/10.28 GB · dit.mnn.weight" */
        val detail: String = "",
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
