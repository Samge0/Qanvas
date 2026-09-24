package com.samge.qanvas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.samge.qanvas.ui.MainViewModel
import com.samge.qanvas.ui.QanvasRoot

class MainActivity : AppCompatActivity() {

    private val notifPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[MainViewModel::class.java]
        // apply persisted language choice before composing
        vm.applyPersistedLocale()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.refreshGate()
        vm.collectResult()
        handleNavExtra(intent)
        setContent {
            com.samge.qanvas.ui.theme.QanvasTheme {
                QanvasRoot(vm)
            }
        }
    }

    /** Notification tap → jump straight to the relevant tab. */
    private fun handleNavExtra(intent: Intent?) {
        // routed through QanvasRoot via a saved-state style hook; simple approach:
        intent?.getIntExtra("open_tab", -1)?.let { tab ->
            if (tab >= 0) openTabRequest = tab
        }
        // Shared image (ACTION_SEND image/*) → load it as the Edit input.
        @Suppress("DEPRECATION")
        val sharedUri = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
        else
            intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        if (intent?.action == Intent.ACTION_SEND && sharedUri != null) {
            ViewModelProvider(this)[MainViewModel::class.java].pickEditImage(sharedUri)
            openTabRequest = 2
        }
    }

    companion object {
        /** last requested tab from a notification tap, consumed by QanvasRoot. */
        @Volatile var openTabRequest: Int = -1
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavExtra(intent)
    }

    override fun onResume() {
        super.onResume()
        // re-check gate (download may have completed via service, storage may have changed)
        ViewModelProvider(this)[MainViewModel::class.java].refreshGate()
    }
}
