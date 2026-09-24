package com.samge.qanvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
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
        setContent {
            com.samge.qanvas.ui.theme.QanvasTheme {
                QanvasRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // re-check gate (download may have completed via service, storage may have changed)
        ViewModelProvider(this)[MainViewModel::class.java].refreshGate()
    }
}
