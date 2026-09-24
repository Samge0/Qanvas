package com.samge.qanvas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samge.qanvas.BuildConfig
import com.samge.qanvas.R
import com.samge.qanvas.core.GenBus
import com.samge.qanvas.core.GenEngine
import com.samge.qanvas.core.GenService
import com.samge.qanvas.core.BgKeepAlive
import com.samge.qanvas.core.ModelMigrator
import com.samge.qanvas.ui.theme.AppleTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun SettingsTab(vm: MainViewModel, gen: GenBus.State) {
    val ctx = LocalContext.current
    val gate by vm.gate.collectAsState()
    val lang by vm.language.collectAsState()
    val prefs = remember { GenEngine.prefs(ctx) }
    val scope = rememberCoroutineScope()

    var proxyOn by rememberSaveable { mutableStateOf(prefs.getBoolean(GenEngine.KEY_PROXY_ENABLED, false)) }
    var proxyHost by rememberSaveable { mutableStateOf(prefs.getString(GenEngine.KEY_PROXY_HOST, "") ?: "") }
    var proxyPort by rememberSaveable { mutableStateOf(prefs.getInt(GenEngine.KEY_PROXY_PORT, 7890).toString()) }

    var migrating by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmResetDir by remember { mutableStateOf(false) }
    var dirPicker by remember { mutableStateOf(false) }
    var pendingDir by remember { mutableStateOf<File?>(null) }
    var migrateChoice by remember { mutableStateOf<File?>(null) }

    val currentCustom: String = remember(gate) { prefs.getString(GenEngine.KEY_MODEL_DIR, "") ?: "" }

    // ---------------- migrate confirmation dialog ----------------
    migrateChoice?.let { newDir ->
        val oldBytes = GenEngine.modelBytes(ctx)
        AlertDialog(
            onDismissRequest = { migrateChoice = null },
            title = { Text(stringResource(R.string.migrate_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.migrate_confirm_body_fmt,
                        String.format(Locale.US, "%.2f GB", oldBytes / 1e9),
                    )
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        migrateChoice = null
                        startMigrate(vm, scope, ctx, newDir, copyThenClean = false) { migrating = it }
                    }) { Text(stringResource(R.string.migrate_move)) }
                    TextButton(onClick = {
                        migrateChoice = null
                        startMigrate(vm, scope, ctx, newDir, copyThenClean = true) { migrating = it }
                    }) { Text(stringResource(R.string.migrate_copy)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { migrateChoice = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    // ---------------- folder picker ----------------
    if (dirPicker) {
        FolderPickerDialog(
            initial = pendingDir ?: GenEngine.modelDir(ctx),
            onDismiss = { dirPicker = false },
            onPicked = { picked ->
                dirPicker = false
                if (picked.isDirectory && picked.canWrite()) {
                    // switch immediately, then offer migration of existing files
                    prefs.edit().putString(GenEngine.KEY_MODEL_DIR, picked.absolutePath).apply()
                    vm.refreshGate()
                    val alreadyThere = GenEngine.missingFiles(ctx) == null
                    if (GenEngine.modelBytes(ctx) > 0 && !alreadyThere) {
                        migrateChoice = picked
                    } else {
                        vm.toast("__saved__")
                    }
                } else {
                    vm.toast("__dir_invalid__")
                }
            },
        )
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
            } else if (gen.kind != GenBus.Kind.DOWNLOADING) {
                Text(
                    stringResource(
                        R.string.set_download_missing_fmt,
                        (GenEngine.missingFiles(ctx) ?: "").take(120),
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
                val detailLine = if (gen.detail == "__preparing__" || gen.detail == "…")
                    stringResource(R.string.dl_preparing) else gen.detail
                Text(
                    detailLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { GenService.requestStop(ctx) },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) { Text(stringResource(R.string.set_download_stop), fontSize = 13.sp, maxLines = 1) }
            } else {
                Button(
                    onClick = { vm.startDownload() },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTokens.ActionBlue),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.set_download_btn), fontSize = 13.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        // ---------------- directory ----------------
        SettingsCard(title = stringResource(R.string.set_dir_title)) {
            Text(
                stringResource(R.string.set_dir_current_fmt, GenEngine.modelDir(ctx).absolutePath),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.set_dir_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        pendingDir = File(currentCustom).takeIf { it.isDirectory }
                        dirPicker = true
                    },
                    enabled = migrating == null && gen.kind != GenBus.Kind.DOWNLOADING,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.FolderOpen, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.set_dir_pick), fontSize = 13.sp, maxLines = 1, softWrap = false)
                }
                if (currentCustom.isNotBlank()) {
                    OutlinedButton(
                        onClick = { confirmResetDir = true },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) { Text(stringResource(R.string.set_dir_reset), fontSize = 13.sp, maxLines = 1, softWrap = false) }
                }
            }
            if (migrating != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.set_migrating_fmt, migrating ?: 0),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                LinearProgressIndicator(
                    progress = { (migrating ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                Switch(
                    checked = proxyOn,
                    onCheckedChange = {
                        proxyOn = it
                        saveProxy(prefs, proxyOn, proxyHost, proxyPort)
                        vm.toast("__saved__")
                    },
                )
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
                        onValueChange = { proxyPort = it.filter { c -> c.isDigit() }.take(5) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.set_proxy_port)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                TextButton(onClick = {
                    saveProxy(prefs, proxyOn, proxyHost, proxyPort)
                    vm.toast("__saved__")
                }) { Text(stringResource(R.string.toast_saved)) }
            }
        }

        // ---------------- language ----------------
        SettingsCard(title = stringResource(R.string.set_lang_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = lang == 0,
                    onClick = { vm.setLanguage(0); vm.toast("__lang__") },
                    label = { Text(stringResource(R.string.set_lang_system)) },
                    shape = RoundedCornerShape(999.dp),
                )
                FilterChip(
                    selected = lang == 1,
                    onClick = { vm.setLanguage(1); vm.toast("__lang__") },
                    label = { Text(stringResource(R.string.set_lang_en)) },
                    shape = RoundedCornerShape(999.dp),
                )
                FilterChip(
                    selected = lang == 2,
                    onClick = { vm.setLanguage(2); vm.toast("__lang__") },
                    label = { Text(stringResource(R.string.set_lang_zh)) },
                    shape = RoundedCornerShape(999.dp),
                )
            }
        }

        // ---------------- performance (hot reload) ----------------
        SettingsCard(title = stringResource(R.string.set_keep_title)) {
            var keep by remember { mutableStateOf(prefs.getBoolean(GenEngine.KEY_KEEP_LOADED, false)) }
            Text(
                stringResource(R.string.set_keep_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.set_keep_warn),
                style = MaterialTheme.typography.bodySmall,
                color = AppleTokens.Orange,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_keep_enable), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = keep,
                    onCheckedChange = {
                        keep = it
                        prefs.edit().putBoolean(GenEngine.KEY_KEEP_LOADED, it).apply()
                        if (!it) GenService.releaseHot()
                        vm.toast("__saved__")
                    },
                )
            }
        }

        // ---------------- background running ----------------
        SettingsCard(title = stringResource(R.string.set_bg_title)) {
            Text(
                stringResource(R.string.set_bg_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            var overlayOk by remember { mutableStateOf(BgKeepAlive.canDrawOverlays(ctx)) }
            var batteryOk by remember { mutableStateOf(BgKeepAlive.isIgnoringBatteryOptimizations(ctx)) }
            // refresh on every recomposition of this card
            androidx.compose.runtime.LaunchedEffect(Unit) {
                while (true) {
                    overlayOk = BgKeepAlive.canDrawOverlays(ctx)
                    batteryOk = BgKeepAlive.isIgnoringBatteryOptimizations(ctx)
                    kotlinx.coroutines.delay(2000)
                }
            }
            if (batteryOk) {
                StatusRow(ok = true, text = stringResource(R.string.set_bg_battery))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, Modifier.size(16.dp), tint = AppleTokens.Orange)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.set_bg_restricted),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { BgKeepAlive.requestBatteryExemption(ctx) }) {
                        Text(stringResource(R.string.set_bg_battery_action))
                    }
                }
            }
            if (overlayOk) {
                StatusRow(ok = true, text = stringResource(R.string.set_bg_overlay_ok))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, Modifier.size(16.dp), tint = AppleTokens.Orange)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.set_bg_overlay_perm),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { BgKeepAlive.requestOverlayPermission(ctx) }) {
                        Text(stringResource(R.string.set_bg_grant))
                    }
                }
            }
            var keepOverlay by remember { mutableStateOf(prefs.getBoolean(GenEngine.KEY_BG_OVERLAY, true)) }
            var keepSilent by remember { mutableStateOf(prefs.getBoolean(GenEngine.KEY_BG_SILENT, true)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_bg_overlay), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = keepOverlay, onCheckedChange = {
                    keepOverlay = it
                    prefs.edit().putBoolean(GenEngine.KEY_BG_OVERLAY, it).apply()
                    vm.toast("__saved__")
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_bg_silent), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = keepSilent, onCheckedChange = {
                    keepSilent = it
                    prefs.edit().putBoolean(GenEngine.KEY_BG_SILENT, it).apply()
                    vm.toast("__saved__")
                })
            }
            Text(
                stringResource(R.string.set_bg_oem_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---------------- danger ----------------
        SettingsCard(title = stringResource(R.string.set_danger_title)) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = gate.modelPresent,
                shape = RoundedCornerShape(999.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Filled.Delete, null, Modifier.size(14.dp), tint = AppleTokens.Red)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.set_delete_model), color = AppleTokens.Red,
                    fontSize = 13.sp, maxLines = 1, softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible)
            }
        }

        // ---------------- about ----------------
        SettingsCard(title = stringResource(R.string.set_about_title)) {
            Text(
                stringResource(R.string.set_about_body, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Language, null, Modifier.size(16.dp), tint = AppleTokens.ActionBlue)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.set_about_repo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleTokens.ActionBlue,
                    modifier = Modifier.clickable {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Samge0/Qanvas")))
                    },
                )
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    // ---------------- dialogs ----------------
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.set_delete_model)) },
            text = { Text(stringResource(R.string.set_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch(Dispatchers.IO) {
                        GenEngine.deleteModels(ctx)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            vm.toast("__saved__")
                            vm.refreshGate()
                        }
                    }
                }) { Text(stringResource(R.string.delete_confirm), color = AppleTokens.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (confirmResetDir) {
        AlertDialog(
            onDismissRequest = { confirmResetDir = false },
            title = { Text(stringResource(R.string.set_dir_reset)) },
            text = { Text(stringResource(R.string.reset_dir_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmResetDir = false
                    prefs.edit().putString(GenEngine.KEY_MODEL_DIR, null).apply()
                    vm.toast("__reset_dir__")
                    vm.refreshGate()
                }) { Text(stringResource(R.string.set_dir_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetDir = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun saveProxy(prefs: android.content.SharedPreferences, on: Boolean, host: String, port: String) {
    prefs.edit()
        .putBoolean(GenEngine.KEY_PROXY_ENABLED, on)
        .putString(GenEngine.KEY_PROXY_HOST, host.trim())
        .putInt(GenEngine.KEY_PROXY_PORT, port.trim().toIntOrNull() ?: 0)
        .apply()
}

private fun startMigrate(
    vm: MainViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    ctx: android.content.Context,
    target: File,
    copyThenClean: Boolean,
    setProgress: (Int?) -> Unit,
) {
    val from = GenEngine.modelDir(ctx).let { cur ->
        // migrate from the OLD location: previous custom dir or the default dir
        if (cur.absolutePath != target.absolutePath) cur
        else File(ctx.getExternalFilesDir(null), GenEngine.MODEL_DIR_NAME)
    }
    setProgress(0)
    scope.launch {
        val ok = ModelMigrator.move(ctx, from, target, copyThenClean) { p -> setProgress(p) }
        setProgress(null)
        vm.toast(if (ok) "__migrate_done__" else "__dir_invalid__")
        vm.refreshGate()
    }
}

// ==================================================================== folder picker (local filesystem)

@Composable
fun FolderPickerDialog(initial: File, onDismiss: () -> Unit, onPicked: (File) -> Unit) {
    // canonical shared-storage root (legacy API returns this reliably on all API levels)
    val storageRoot = remember {
        runCatching { java.io.File("/storage/emulated/0") }
            .getOrNull()?.takeIf { it.isDirectory }
            ?: android.os.Environment.getExternalStorageDirectory()
            ?: java.io.File("/")
    }
    var current by remember { mutableStateOf(initial.takeIf { it.isDirectory } ?: storageRoot) }
    var entries by remember(current) {
        mutableStateOf(
            current.listFiles { f -> f.isDirectory }?.sortedBy { it.name.lowercase(Locale.ROOT) } ?: emptyList()
        )
    }
    var chosen by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dir_pick_title)) },
        text = {
            Column {
                Text(
                    current.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (current.parentFile != null && current.parentFile!!.canRead()) {
                        TextButton(onClick = {
                            current = current.parentFile!!
                            chosen = current
                        }) {
                            Icon(Icons.Filled.ArrowUpward, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.dir_pick_up))
                        }
                    }
                    if (current != storageRoot) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (storageRoot.isDirectory) { current = storageRoot; chosen = storageRoot }
                            },
                            label = { Text(stringResource(R.string.dir_chip_default)) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.height(280.dp)) {
                    items(entries) { dir ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    chosen = dir
                                    if (dir.canRead()) { current = dir }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (dir == chosen) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                                null, Modifier.size(20.dp),
                                tint = if (dir == chosen) AppleTokens.ActionBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                dir.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPicked(chosen) }) { Text(stringResource(R.string.dir_pick_use)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
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
