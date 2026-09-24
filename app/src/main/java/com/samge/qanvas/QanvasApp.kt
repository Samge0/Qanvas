package com.samge.qanvas

import android.app.Application
import com.samge.qanvas.data.HistoryDb

class QanvasApp : Application() {
    val db: HistoryDb by lazy { HistoryDb.build(this) }
}
