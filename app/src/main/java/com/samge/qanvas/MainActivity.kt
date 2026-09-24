package com.samge.qanvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samge.qanvas.ui.MainViewModel
import com.samge.qanvas.ui.QanvasRoot

class MainActivity : ComponentActivity() {

    private val notifPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val vm = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
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
        val vm = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        vm.refreshGate()
    }
}
