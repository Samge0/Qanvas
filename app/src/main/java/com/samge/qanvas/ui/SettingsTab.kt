package com.samge.qanvas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.samge.qanvas.R
import com.samge.qanvas.core.GenBus
import com.samge.qanvas.core.GenEngine
import com.samge.qanvas.core.GenService
import com.samge.qanvas.core.ModelMigrator
import com.samge.qanvas.ui.theme.AppleTokens
import java.util.Locale

@Composable
fun SettingsTab(vm: MainViewModel, gen: GenBus.State) {
    val ctx = LocalContext.current
    val gate by vm.gate.collectAsState()
    val prefs = remember { GenEngine.prefs(ctx) }

    var customDir by rememberSaveable { mutableStateOf(prefs.getString(GenEngine.KEY_MODEL_DIR, "") ?: "") }
    var proxyOn by rememberSaveable { mutableStateOf(prefs.getBoolean(GenEngine.KEY_PROXY_ENABLED, false)) }
    var proxyHost by rememberSaveable { mutableStateOf(prefs.getString(GenEngine.KEY_PROXY_HOST, "") ?: "") }
    var proxyPort by rememberSaveable { mutableStateOf(prefs.getInt(GenEngine.KEY_PROXY_PORT, 7890).toString()) }
    var migrating by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val lang by vm.language.collectAsState()

    fun saveProxy() {
        prefs.edit()
            .putBoolean(GenEngine.KEY_PROXY_ENABLED, proxyOn)
            .putString(GenEngine.KEY_PROXY_HOST, proxyHost.trim())
            .putInt(GenEngine.KEY_PROXY_PORT, proxyPort.trim().toIntOrNull() ?: 0)
            .apply()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // ---------------- download ----------------
        SettingsCard(title = stringResource(R.string.set_download_title)) {
            Text(
                stringResource(R.string.set_download_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            if (gate.modelPresent) {
                StatusRow(ok = true, text = stringResource(R.string.set_download_done))
            } else {
                Text(
                    stringResource(
                        R.string.set_download_missing_fmt,
                        GenEngine.missingFiles(ctx)?.take(120) ?: "",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = AppleTokens.Orange,
                )
            }
            if (gen.kind == GenBus.Kind.DOWNLOADING) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { gen.progress / 100f },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("${gen.progress}%", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    "${gen.stage}\n${gen.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { GenService.requestStop(ctx) },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.set_download_stop)) }
            } else {
                Button(
                    onClick = { vm.startDownload() },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTokens.ActionBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (gate.modelPresent) stringResource(R.string.set_download_btn) + "  ✓"
                        else stringResource(R.string.set_download_btn)
                    )
                }
            }
        }

        // ---------------- directory ----------------
        SettingsCard(title = stringResource(R.string.set_dir_title)) {
            Text(
                stringResource(R.string.set_dir_current_fmt, GenEngine.modelDir(ctx).absolutePath),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = customDir,
                onValueChange = { customDir = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.set_dir_hint), style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(12.dp),
            )
            Text(
                stringResource(R.string.set_dir_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val f = java.io.File(customDir.trim())
                        if (customDir.isBlank() || !f.isDirectory || !f.canWrite()) {
                            vm.toast("__dir_invalid__")
                        } else {
                            prefs.edit().putString(GenEngine.KEY_MODEL_DIR, customDir.trim()).apply()
                            vm.toast("__dir_applied__")
                            vm.refreshGate()
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                ) { Text(stringResource(R.string.set_dir_apply)) }
                OutlinedButton(
                    onClick = {
                        prefs.edit().putString(GenEngine.KEY_MODEL_DIR, null).apply()
                        customDir = ""
                        vm.toast("__dir_applied__")
                        vm.refreshGate()
                    },
                    shape = RoundedCornerShape(999.dp),
                ) { Text(stringResource(R.string.set_dir_reset)) }
            }
            if (customDir.isNotBlank() && gate.modelPresent) {
                val scope = rememberCoroutineScope()
                OutlinedButton(
                    onClick = {
                        val from = java.io.File(ctx.getExternalFilesDir(null), GenEngine.MODEL_DIR_NAME)
                        val to = java.io.File(customDir.trim())
                        migrating = 0
                        scope.launch {
                            val ok = ModelMigrator.move(ctx, from, to) { p -> migrating = p }
                            migrating = null
                            vm.toast(if (ok) "__migrate_done__" else "__dir_invalid__")
                            vm.refreshGate()
                        }
                    },
                    enabled = migrating == null && gen.kind != GenBus.Kind.DOWNLOADING,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    if (migrating != null) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.set_migrating_fmt, migrating ?: 0))
                    } else {
                        Icon(Icons.Filled.DriveFileMove, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.set_dir_migrate))
                    }
                }
            }
        }

        // ---------------- proxy ----------------
        SettingsCard(title = stringResource(R.string.set_proxy_title)) {
            Text(
                stringResource(R.string.set_proxy_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_proxy_enable), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(checked = proxyOn, onCheckedChange = { proxyOn = it; saveProxy() })
            }
            if (proxyOn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proxyHost,
                        onValueChange = { proxyHost = it },
                        modifier = Modifier.weight(2f),
                        placeholder = { Text(stringResource(R.string.set_proxy_host)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedTextField(
                        value = proxyPort,
                        onValueChange = { proxyPort = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.set_proxy_port)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                TextButton(onClick = { saveProxy() }) { Text("Save") }
            }
        }

        // ---------------- language ----------------
        SettingsCard(title = stringResource(R.string.set_lang_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = lang == 0,
                    onClick = { vm.setLanguage(0) },
                    label = { Text(stringResource(R.string.set_lang_system)) },
                    shape = RoundedCornerShape(999.dp),
                )
                FilterChip(
                    selected = lang == 1,
                    onClick = { vm.setLanguage(1) },
                    label = { Text(stringResource(R.string.set_lang_en)) },
                    shape = RoundedCornerShape(999.dp),
                )
                FilterChip(
                    selected = lang == 2,
                    onClick = { vm.setLanguage(2) },
                    label = { Text(stringResource(R.string.set_lang_zh)) },
                    shape = RoundedCornerShape(999.dp),
                )
            }
        }

        // ---------------- danger ----------------
        SettingsCard(title = stringResource(R.string.set_danger_title)) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = gate.modelPresent,
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(Icons.Filled.Delete, null, Modifier.size(16.dp), tint = AppleTokens.Red)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.set_delete_model), color = AppleTokens.Red)
            }
        }

        // ---------------- about ----------------
        SettingsCard(title = stringResource(R.string.set_about_title)) {
            Text(
                stringResource(R.string.set_about_body, "1.1.0"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(30.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.set_delete_model)) },
            text = { Text(stringResource(R.string.set_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Thread { GenEngine.deleteModels(ctx) }.start()
                    vm.refreshGate()
                }) { Text(stringResource(R.string.delete_confirm), color = AppleTokens.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StatusRow(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.CheckCircle, null, Modifier.size(16.dp),
            tint = if (ok) AppleTokens.Green else AppleTokens.Orange,
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
