package com.samge.qanvas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samge.qanvas.R
import com.samge.qanvas.core.GenBus
import com.samge.qanvas.core.GenEngine
import com.samge.qanvas.core.GenService
import com.samge.qanvas.core.Inspo
import com.samge.qanvas.core.QwenImage21SizeProxy
import com.samge.qanvas.data.GenRecord
import com.samge.qanvas.ui.theme.AppleTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TAB_KEYS = listOf(R.string.tab_create, R.string.tab_sticker, R.string.tab_edit, R.string.tab_gallery, R.string.tab_inspo, R.string.tab_settings)

// ==================================================================== root / nav

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QanvasRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val gate by vm.gate.collectAsState()
    val gen by vm.gen.collectAsState()
    val toastMsg by vm.toast.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    // toast keys → localized snackbar
    LaunchedEffect(toastMsg) {
        when (toastMsg) {
            "__need_download__" -> snackbar.showSnackbar(ctx.getString(R.string.toast_need_download))
            "__dir_invalid__" -> snackbar.showSnackbar(ctx.getString(R.string.toast_dir_invalid))
            "__dir_applied__", "__saved__" -> snackbar.showSnackbar(ctx.getString(R.string.toast_saved))
            "__migrate_done__" -> snackbar.showSnackbar(ctx.getString(R.string.toast_migrate_done))
            "__reset_dir__" -> snackbar.showSnackbar(ctx.getString(R.string.toast_reset_dir))
            "__lang__" -> snackbar.showSnackbar(ctx.getString(R.string.toast_lang_set))
        }
        if (toastMsg != null) vm.toastShown()
    }

    Scaffold(
        topBar = { QanvasTopBar(gate, onSettings = { tab = 5 }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // scrollable single-line tab bar: never wraps to two lines on narrow screens
            @OptIn(ExperimentalMaterial3Api::class)
            androidx.compose.material3.SecondaryScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 8.dp,
            ) {
                TAB_KEYS.forEachIndexed { i, res ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Text(
                                stringResource(res),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
            when (tab) {
                0 -> if (gate.modelPresent) CreateTab(vm, gen) else MissingModelGate(vm)
                1 -> if (gate.modelPresent) StickerTab(vm, gen) else MissingModelGate(vm)
                2 -> if (gate.modelPresent) EditTab(vm, gen) else MissingModelGate(vm)
                3 -> GalleryTab(vm)
                4 -> InspoTab()
                5 -> SettingsTab(vm, gen)
            }
        }
    }
}

@Composable
private fun QanvasTopBar(gate: GateStatus, onSettings: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AppleTokens.Violet, AppleTokens.ActionBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Text("Q", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Qanvas", style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.top_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            if (gate.modelPresent) {
                Icon(
                    Icons.Filled.CheckCircle, stringResource(R.string.model_ready),
                    tint = AppleTokens.Green, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.model_ready),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Icon(
                    Icons.Filled.Warning, stringResource(R.string.model_missing),
                    tint = AppleTokens.Orange, modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, stringResource(R.string.tab_settings))
            }
        }
    }
}

// ==================================================================== shared controls

/** Full-page redirect for Create/Sticker/Edit when the model isn't installed yet. */
@Composable
fun MissingModelGate(vm: MainViewModel) {
    val gate by vm.gate.collectAsState()
    val gen by vm.gen.collectAsState()
    val dl = gen.kind == GenBus.Kind.DOWNLOADING

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall, lineHeight = 28.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.home_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeaturePill(Icons.Filled.Palette, stringResource(R.string.feature_t2i))
                FeaturePill(Icons.Filled.Edit, stringResource(R.string.feature_edit))
                FeaturePill(Icons.Filled.Star, stringResource(R.string.feature_sticker))
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.gate_title), style = MaterialTheme.typography.titleMedium)
                    GateRow(
                        ok = true,
                        label = stringResource(
                            R.string.ram_fmt,
                            if (gate.ramMB > 0) String.format(Locale.US, "%.1f", gate.ramMB / 1024.0) + " GB" else "…",
                        ),
                        note = stringResource(if (gate.ramOk) R.string.ram_ok else R.string.ram_low),
                        icon = { Icon(Icons.Filled.Memory, null, tint = it) },
                    )
                    GateRow(
                        ok = gate.storageOk,
                        label = stringResource(
                            R.string.storage_fmt,
                            String.format(Locale.US, "%.1f", gate.freeBytes / 1e9),
                        ),
                        note = stringResource(if (gate.storageOk) R.string.storage_ok else R.string.storage_low),
                        icon = { Icon(Icons.Filled.CheckCircle, null, tint = it) },
                    )
                    Text(
                        stringResource(R.string.gate_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (dl) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.notif_downloading) + " ${gen.progress}%",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { GenService.requestStop(vm.getApplication()) }) {
                                Text(stringResource(R.string.set_download_stop))
                            }
                        }
                        LinearProgressIndicator(
                            progress = { gen.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            gen.detail,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = { vm.startDownload() },
                enabled = !dl,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppleTokens.ActionBlue),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.set_download_btn), fontWeight = FontWeight.Medium)
            }
        }
        item {
            OutlinedButton(
                onClick = { },
                enabled = false,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("adb push → " + GenEngine.MODEL_DIR_NAME) }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AppleTokens.VioletSoft,
        border = BorderStroke(1.dp, Color(0x1A6C5CE7)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = AppleTokens.Violet)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppleTokens.Violet)
        }
    }
}

@Composable
private fun GateRow(ok: Boolean, label: String, note: String, icon: @Composable (Color) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val tint = if (ok) AppleTokens.Green else AppleTokens.Orange
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { icon(tint) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatioTierPicker(vm: MainViewModel) {
    val ratio by vm.ratioOrdinal.collectAsState()
    val tier by vm.tierOrdinal.collectAsState()
    Text(stringResource(R.string.ratio_label), style = MaterialTheme.typography.labelMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf("1:1", "4:3", "3:4", "3:2", "2:3", "16:9", "9:16").forEachIndexed { i, lbl ->
            FilterChip(
                selected = ratio == i,
                onClick = { vm.ratioOrdinal.value = i },
                label = { Text(lbl, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(999.dp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.tier_label), style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.tier_standard, R.string.tier_fast, R.string.tier_tiny).forEachIndexed { i, res ->
            FilterChip(
                selected = tier == i,
                onClick = { vm.tierOrdinal.value = i },
                label = { Text(stringResource(res), style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(999.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepsSeedRow(vm: MainViewModel, hint: String) {
    val steps by vm.steps.collectAsState()
    val seed by vm.seedText.collectAsState()
    Text(stringResource(R.string.steps_label), style = MaterialTheme.typography.labelMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(12, 16, 20, 28).forEach { s ->
            FilterChip(
                selected = steps == s,
                onClick = { vm.steps.value = s },
                label = { Text("$s") },
                shape = RoundedCornerShape(999.dp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.seed_label), style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = seed,
            onValueChange = { vm.seedText.value = it.filter { c -> c.isDigit() }.take(9) },
            modifier = Modifier.width(140.dp).height(52.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp),
        )
    }
    Text(
        hint,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ProgressCard(gen: GenBus.State, estimateSec: Int) {
    val stageText = when (gen.stageKey.ifEmpty { gen.stage }) {
        "load" -> stringResource(R.string.notif_stage_load)
        "te" -> stringResource(R.string.notif_stage_te)
        "vae" -> stringResource(R.string.notif_stage_vae)
        "denoise" -> stringResource(R.string.notif_stage_denoise, gen.stageStep, 20)
        "" -> stringResource(R.string.progress_preparing)
        else -> gen.stage // download filename
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stageText, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${gen.progress}%", style = MaterialTheme.typography.titleMedium, color = AppleTokens.ActionBlue)
            }
            LinearProgressIndicator(
                progress = { gen.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.progress_note_fmt, estimateSec / 60),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ResultCard(bitmap: android.graphics.Bitmap?, checker: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier.fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (checker) Color(0xFFE8E8ED) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (checker) Color(0xFFD5D5DC) else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        stringResource(R.string.result_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ==================================================================== tabs (Create / Sticker / Edit)

@Composable
fun CreateTab(vm: MainViewModel, gen: GenBus.State) {
    val ratio by vm.ratioOrdinal.collectAsState()
    val tier by vm.tierOrdinal.collectAsState()
    val steps by vm.steps.collectAsState()
    val result by vm.result.collectAsState()
    var prompt by rememberSaveable { mutableStateOf("") }
    val modelReady = vm.gate.collectAsState().value.modelPresent

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text(stringResource(R.string.create_placeholder), style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(14.dp),
            )
        }
        item { RatioTierPicker(vm) }
        item {
            val size = QwenImage21SizeProxy.of(ratio, tier)
            StepsSeedRow(
                vm,
                stringResource(R.string.size_fmt, size.width, size.height, size.tokens(), GenEngine.estimateSeconds(size.tokens(), steps) / 60),
            )
        }
        item {
            GenerateButton(
                enabled = prompt.isNotBlank() && !GenServiceRunning(gen) && modelReady,
                running = GenServiceRunning(gen),
                modelReady = modelReady,
            ) { vm.startGeneration(prompt, "t2i", null) }
        }
        if (GenServiceRunning(gen)) {
            item {
                val size = QwenImage21SizeProxy.of(ratio, tier)
                ProgressCard(gen, GenEngine.estimateSeconds(size.tokens(), steps))
            }
        }
        if (gen.kind == GenBus.Kind.OOM) item { OomCard() }
        if (gen.kind == GenBus.Kind.ERROR) item { ErrorCard(gen.error ?: "unknown") }
        item { ResultCard(result) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickerTab(vm: MainViewModel, gen: GenBus.State) {
    val ratio by vm.ratioOrdinal.collectAsState()
    val tier by vm.tierOrdinal.collectAsState()
    val steps by vm.steps.collectAsState()
    val result by vm.result.collectAsState()
    val modelReady = vm.gate.collectAsState().value.modelPresent
    var prompt by rememberSaveable { mutableStateOf(Inspo.byKind(Inspo.Kind.STICKER).first().prompt) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = AppleTokens.VioletSoft) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = AppleTokens.Violet)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.sticker_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleTokens.Violet,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text(stringResource(R.string.sticker_placeholder), style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(14.dp),
            )
        }
        item { RatioTierPicker(vm) }
        item {
            val size = QwenImage21SizeProxy.of(ratio, tier)
            StepsSeedRow(
                vm,
                stringResource(R.string.size_fmt, size.width, size.height, size.tokens(), GenEngine.estimateSeconds(size.tokens(), steps) / 60),
            )
        }
        item {
            GenerateButton(
                enabled = prompt.isNotBlank() && !GenServiceRunning(gen) && modelReady,
                running = GenServiceRunning(gen),
                modelReady = modelReady,
            ) { vm.startGeneration(prompt, "t2i", null) }
        }
        if (GenServiceRunning(gen)) {
            item {
                val size = QwenImage21SizeProxy.of(ratio, tier)
                ProgressCard(gen, GenEngine.estimateSeconds(size.tokens(), steps))
            }
        }
        if (gen.kind == GenBus.Kind.OOM) item { OomCard() }
        if (gen.kind == GenBus.Kind.ERROR) item { ErrorCard(gen.error ?: "unknown") }
        item { ResultCard(result, checker = true) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
fun EditTab(vm: MainViewModel, gen: GenBus.State) {
    val editInput by vm.editInput.collectAsState()
    val tier by vm.tierOrdinal.collectAsState()
    val steps by vm.steps.collectAsState()
    val result by vm.result.collectAsState()
    val modelReady = vm.gate.collectAsState().value.modelPresent
    var prompt by rememberSaveable { mutableStateOf("") }
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) vm.pickEditImage(uri) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.edit_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.edit_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            if (editInput == null) {
                Surface(
                    onClick = { pick.launch("image/*") },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(36.dp), tint = AppleTokens.ActionBlue)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.edit_pick_title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.edit_pick_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.edit_input_fmt, editInput!!.width, editInput!!.height),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { vm.clearEditImage() }) {
                                Text(stringResource(R.string.edit_remove))
                            }
                        }
                        val out = QwenImage21SizeProxy.editSize(editInput!!.width, editInput!!.height, tier)
                        Text(
                            stringResource(R.string.edit_output_fmt, out[0], out[1]),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text(stringResource(R.string.edit_placeholder), style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(14.dp),
            )
        }
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = AppleTokens.VioletSoft) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, Modifier.size(16.dp), tint = AppleTokens.Violet)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.fast_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleTokens.Violet,
                    )
                }
            }
        }
        item {
            Text(stringResource(R.string.pixel_budget_label), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(R.string.tier_standard, R.string.tier_fast, R.string.tier_tiny).forEachIndexed { i, res ->
                    FilterChip(
                        selected = tier == i,
                        onClick = { vm.tierOrdinal.value = i },
                        label = { Text(stringResource(res)) },
                        shape = RoundedCornerShape(999.dp),
                    )
                }
            }
        }
        item { StepsSeedRow(vm, stringResource(R.string.edit_est_fmt, GenEngine.estimateSeconds(800, steps) / 60)) }
        item {
            GenerateButton(
                enabled = editInput != null && prompt.isNotBlank() && !GenServiceRunning(gen) && modelReady,
                running = GenServiceRunning(gen),
                modelReady = modelReady,
            ) { vm.startGeneration(prompt, "edit", editInput) }
        }
        if (GenServiceRunning(gen)) {
            item { ProgressCard(gen, GenEngine.estimateSeconds(800, steps)) }
        }
        if (gen.kind == GenBus.Kind.OOM) item { OomCard() }
        if (gen.kind == GenBus.Kind.ERROR) item { ErrorCard(gen.error ?: "unknown") }
        item { ResultCard(result) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

// ==================================================================== gallery & inspo

@Composable
fun GalleryTab(vm: MainViewModel) {
    val history by vm.history.collectAsState()
    var confirmDelete by remember { mutableStateOf<GenRecord?>(null) }

    if (history.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.gallery_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.gallery_empty_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(history, key = { it.id }) { rec ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    val bmp = remember(rec.outPath) {
                        try {
                            val o = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                            android.graphics.BitmapFactory.decodeFile(rec.outPath, o)
                        } catch (e: Exception) { null }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(), contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        rec.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.rec_meta_fmt, rec.mode, rec.width, rec.height, rec.steps, rec.seed),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                    )
                    Text(
                        stringResource(R.string.rec_meta2_fmt, fmt.format(Date(rec.createdAt)), rec.durationMs / 1000),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        TextButton(onClick = { confirmDelete = rec }) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.delete_confirm))
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(rec.prompt) },
            confirmButton = {
                TextButton(onClick = { vm.deleteRecord(rec); confirmDelete = null }) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InspoTab() {
    val zh = remember { Inspo.isZh() }
    val groupTitles = listOf(
        stringResource(R.string.inspo_t2i),
        stringResource(R.string.inspo_sticker),
        stringResource(R.string.inspo_edit),
        stringResource(R.string.inspo_poster),
    )
    val kinds = listOf(Inspo.Kind.T2I, Inspo.Kind.STICKER, Inspo.Kind.EDIT, Inspo.Kind.POSTER)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        kinds.forEachIndexed { gi, kind ->
            item(span = { GridItemSpan(2) }) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(groupTitles[gi], style = MaterialTheme.typography.titleMedium)
                }
            }
            items(Inspo.byKind(kind)) { card ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(14.dp), tint = AppleTokens.Violet)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                card.titleFor(zh),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (card.hintFor(zh).isNotEmpty()) {
                            Text(card.hintFor(zh), style = MaterialTheme.typography.bodySmall, color = AppleTokens.Violet)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            card.promptFor(zh), maxLines = 4, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

// ==================================================================== small bits

fun GenServiceRunning(gen: GenBus.State): Boolean =
    gen.kind == GenBus.Kind.GENERATING || gen.kind == GenBus.Kind.LOADING

@Composable
fun GenerateButton(enabled: Boolean, running: Boolean, modelReady: Boolean, onClick: () -> Unit) {
    Button(
        onClick = {
            if (modelReady) onClick()
        },
        enabled = enabled || (running && modelReady),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppleTokens.ActionBlue),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (running) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_generate_running), fontWeight = FontWeight.Medium)
        } else {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_generate), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OomCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x14FF9500)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, Modifier.size(18.dp), tint = AppleTokens.Orange)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.oom_title), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.oom_body), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ErrorCard(msg: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x14D70015)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.err_title), style = MaterialTheme.typography.titleMedium, color = AppleTokens.Red)
            Spacer(Modifier.height(6.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}
