package com.samge.qanvas.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samge.qanvas.core.GenBus
import com.samge.qanvas.core.GenEngine
import com.samge.qanvas.core.Inspo
import com.samge.qanvas.data.GenRecord
import com.samge.qanvas.ui.theme.AppleTokens
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==================================================================== root / nav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QanvasRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val gate by vm.gate.collectAsState()
    val gen by vm.gen.collectAsState()

    Scaffold(topBar = { QanvasTopBar(gate) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (!gate.modelPresent && gen.kind != GenBus.Kind.DOWNLOADING) {
                GateScreen(vm, gate, gen)
            } else {
                SecondaryTabRow(selectedTabIndex = tab) {
                    listOf("Create", "Sticker", "Edit", "Gallery", "Inspo").forEachIndexed { i, label ->
                        Tab(
                            selected = tab == i,
                            onClick = { tab = i },
                            text = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
                when (tab) {
                    0 -> CreateTab(vm, gen)
                    1 -> StickerTab(vm, gen)
                    2 -> EditTab(vm, gen)
                    3 -> GalleryTab(vm)
                    4 -> InspoTab()
                }
            }
        }
    }
}

@Composable
private fun QanvasTopBar(gate: com.samge.qanvas.ui.GateStatus) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AppleTokens.Violet, AppleTokens.ActionBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Text("Q", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Qanvas", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Qwen-Image-2.1 · 7B · on-device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            if (gate.modelPresent) {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = AppleTokens.Green, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Model ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ==================================================================== gate / download

@Composable
fun GateScreen(vm: MainViewModel, gate: GateStatus, gen: GenBus.State) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("Private AI art studio,\nentirely on your phone.",
                style = MaterialTheme.typography.headlineSmall, lineHeight = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Qwen-Image-2.1 · 7B diffusion transformer, int4 on OpenCL. No cloud, no upload, no account — your photos never leave the device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeaturePill(Icons.Filled.Palette, "Text → Image")
                FeaturePill(Icons.Filled.Edit, "Photo Editing")
                FeaturePill(Icons.Filled.Category, "RGBA Stickers")
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("One-time model setup", style = MaterialTheme.typography.titleMedium)
                    GateRow(
                        ok = true,
                        label = "RAM ${if (gate.ramMB > 0) "${gate.ramMB / 1024.0} GB".let { String.format(Locale.US, "%.1f", gate.ramMB / 1024.0) + " GB" } else "…"}",
                        note = if (gate.ramOk) "recommended class" else "12 GB+ recommended — use Fast/Tiny tiers",
                        icon = { Icon(Icons.Filled.Memory, null, tint = it) },
                    )
                    GateRow(
                        ok = gate.storageOk,
                        label = "Free storage ${
                            String.format(Locale.US, "%.1f", gate.freeBytes / 1e9)
                        } / 11 GB needed",
                        note = if (gate.storageOk) "enough space" else "free up space to continue",
                        icon = { Icon(Icons.Filled.CheckCircle, null, tint = it) },
                    )
                    Text(
                        "Model download ~10.3 GB from Hugging Face · resumable · checksum-verified. Wi-Fi strongly recommended.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (gen.kind == GenBus.Kind.DOWNLOADING) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Downloading… ${gen.progress}%", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { com.samge.qanvas.core.GenService.requestStop(vm.getApplication()) }) {
                                Text("Stop")
                            }
                        }
                        LinearProgressIndicator(
                            progress = { gen.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${gen.stage}\n${gen.detail}",
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
                enabled = gen.kind != GenBus.Kind.DOWNLOADING,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppleTokens.ActionBlue),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download model · 10.3 GB", fontWeight = FontWeight.Medium)
            }
        }
        item {
            OutlinedButton(
                onClick = { /* placeholder — wired in CreateTab via vm */ },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = false,
            ) {
                Text("Push models via adb instead (see README)")
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AppleTokens.VioletSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1A6C5CE7)),
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
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { icon(tint) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                note, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ==================================================================== shared controls

@Composable
fun RatioTierPicker(vm: MainViewModel, ratioLabels: Array<String>, tierNotes: Array<String>) {
    val ratio by vm.ratioOrdinal.collectAsState()
    val tier by vm.tierOrdinal.collectAsState()
    Text("Aspect ratio", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        ratioLabels.forEachIndexed { i, lbl ->
            FilterChip(
                selected = ratio == i,
                onClick = { vm.ratioOrdinal.value = i },
                label = { Text(lbl, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppleTokens.ActionBlue.copy(alpha = 0.12f),
                    selectedLabelColor = AppleTokens.ActionBlue,
                ),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Text("Quality tier", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tierNotes.forEachIndexed { i, lbl ->
            FilterChip(
                selected = tier == i,
                onClick = { vm.tierOrdinal.value = i },
                label = { Text(lbl, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppleTokens.ActionBlue.copy(alpha = 0.12f),
                    selectedLabelColor = AppleTokens.ActionBlue,
                ),
            )
        }
    }
}

@Composable
fun StepsSeedRow(vm: MainViewModel, tokensHint: String) {
    val steps by vm.steps.collectAsState()
    val seed by vm.seedText.collectAsState()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Steps", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12, 16, 20, 28).forEach { s ->
                    FilterChip(
                        selected = steps == s,
                        onClick = { vm.steps.value = s },
                        label = { Text("$s") },
                        shape = RoundedCornerShape(999.dp),
                    )
                }
            }
        }
        Column(Modifier.width(120.dp)) {
            Text("Seed", style = MaterialTheme.typography.labelMedium)
            var txt by remember(seed) { mutableStateOf(seed ?: "42") }
            androidx.compose.material3.OutlinedTextField(
                value = txt,
                onValueChange = { txt = it; vm.seedText.value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Text(
        tokensHint,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ProgressCard(gen: GenBus.State, estimateSec: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(gen.stage.ifEmpty { "Preparing…" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${gen.progress}%", style = MaterialTheme.typography.titleMedium, color = AppleTokens.ActionBlue)
            }
            LinearProgressIndicator(
                progress = { gen.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "~${estimateSec / 60} min on SD 8 Gen 2-class hardware · notification keeps progress when you leave",
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (checker) Color(0xFFE8E8ED) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        if (checker) Color(0xFFD5D5DC) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        "Result appears here",
                        Modifier.padding(40.dp),
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

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            PromptField(prompt, { prompt = it }, "Describe the image…  e.g. \"A neon sign that reads QWEN, rainy night\"")
        }
        item {
            RatioTierPicker(
                vm,
                arrayOf("1:1", "4:3", "3:4", "3:2", "2:3", "16:9", "9:16"),
                arrayOf("Standard", "Fast", "Tiny"),
            )
        }
        item {
            val size = com.samge.qanvas.core.QwenImage21SizeProxy.of(ratio, tier)
            StepsSeedRow(
                vm,
                "Output ${size.width}×${size.height} · ${size.tokens()} latent tokens/step · ~${
                    GenEngine.estimateSeconds(size.tokens(), steps) / 60
                } min est.",
            )
        }
        item {
            GenerateButton(
                enabled = prompt.isNotBlank() && !GenServiceRunning(gen),
                running = GenServiceRunning(gen),
            ) { vm.startGeneration(prompt, "t2i", null) }
        }
        if (GenServiceRunning(gen)) {
            item {
                val size = com.samge.qanvas.core.QwenImage21SizeProxy.of(ratio, tier)
                ProgressCard(gen, GenEngine.estimateSeconds(size.tokens(), steps))
            }
        }
        if (gen.kind == GenBus.Kind.OOM) {
            item { OomCard() }
        }
        if (gen.kind == GenBus.Kind.ERROR) {
            item { ErrorCard(gen.error ?: "unknown") }
        }
        item { ResultCard(result) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
fun StickerTab(vm: MainViewModel, gen: GenBus.State) {
    val ratio by vm.ratioOrdinal.collectAsState()
    val tier by vm.tierOrdinal.collectAsState()
    val steps by vm.steps.collectAsState()
    val result by vm.result.collectAsState()
    var prompt by rememberSaveable { mutableStateOf(Inspo.byKind(Inspo.Kind.STICKER).first().prompt) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AppleTokens.VioletSoft,
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = AppleTokens.Violet)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Native RGBA transparency — unique to Qwen-Image-2.1 on-device. Exports real transparent PNG stickers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleTokens.Violet,
                    )
                }
            }
        }
        item { PromptField(prompt, { prompt = it }, "Sticker subject…  keep the RGBA template text") }
        item {
            RatioTierPicker(
                vm,
                arrayOf("1:1", "4:3", "3:4", "3:2", "2:3", "16:9", "9:16"),
                arrayOf("Standard", "Fast", "Tiny"),
            )
        }
        item {
            val size = com.samge.qanvas.core.QwenImage21SizeProxy.of(ratio, tier)
            StepsSeedRow(
                vm,
                "Output ${size.width}×${size.height} · ${size.tokens()} tokens/step · ~${
                    GenEngine.estimateSeconds(size.tokens(), steps) / 60
                } min est.",
            )
        }
        item {
            GenerateButton(
                enabled = prompt.isNotBlank() && !GenServiceRunning(gen),
                running = GenServiceRunning(gen),
            ) { vm.startGeneration(prompt, "t2i", null) }
        }
        if (GenServiceRunning(gen)) {
            item {
                val size = com.samge.qanvas.core.QwenImage21SizeProxy.of(ratio, tier)
                ProgressCard(gen, GenEngine.estimateSeconds(size.tokens(), steps))
            }
        }
        if (gen.kind == GenBus.Kind.OOM) item { OomCard() }
        if (gen.kind == GenBus.Kind.ERROR) item { ErrorCard(gen.error ?: "unknown") }
        item { ResultCard(result, checker = true) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTab(vm: MainViewModel, gen: GenBus.State) {
    val editInput by vm.editInput.collectAsState()
    val tier by vm.tierOrdinal.collectAsState()
    val steps by vm.steps.collectAsState()
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
            Text("AI photo editing", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pick a photo, describe the change. Output keeps the input's aspect ratio at the tier budget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            // input picker / preview
            if (editInput == null) {
                Surface(
                    onClick = { pick.launch("image/*") },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.PhotoLibrary, null,
                            Modifier.size(36.dp), tint = AppleTokens.ActionBlue,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Choose a photo to edit", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "stays on your device — never uploaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Input ${editInput!!.width}×${editInput!!.height}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { vm.clearEditImage() }) {
                                Icon(Icons.Filled.Delete, "Remove", Modifier.size(18.dp))
                            }
                        }
                        val out = com.samge.qanvas.core.QwenImage21SizeProxy.editSize(
                            editInput!!.width, editInput!!.height, tier,
                        )
                        Text(
                            "→ output ${out[0]}×${out[1]} at this tier",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            PromptField(prompt, { prompt = it }, "Describe the edit…  e.g. \"Change the background to a sunset beach\"")
        }
        item {
            // Fast-default hint (D2 decision): Fast keeps faces better
            Surface(shape = RoundedCornerShape(14.dp), color = AppleTokens.VioletSoft) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, Modifier.size(16.dp), tint = AppleTokens.Violet)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Tip: Fast tier keeps faces & identity better than Standard (measured upstream); it's the default here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleTokens.Violet,
                    )
                }
            }
        }
        item {
            Text("Pixel budget", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                arrayOf("Standard", "Fast", "Tiny").forEachIndexed { i, lbl ->
                    val t by vm.tierOrdinal.collectAsState()
                    FilterChip(
                        selected = t == i,
                        onClick = { vm.tierOrdinal.value = i },
                        label = { Text(lbl) },
                        shape = RoundedCornerShape(999.dp),
                    )
                }
            }
        }
        item {
            StepsSeedRow(vm, "~${com.samge.qanvas.core.GenEngine.estimateSeconds(800, steps) / 60}+ min est. for edits")
        }
        item {
            GenerateButton(
                enabled = editInput != null && prompt.isNotBlank() && !GenServiceRunning(gen),
                running = GenServiceRunning(gen),
            ) { vm.startGeneration(prompt, "edit", editInput) }
        }
        if (GenServiceRunning(gen)) {
            item { ProgressCard(gen, com.samge.qanvas.core.GenEngine.estimateSeconds(800, steps)) }
        }
        if (gen.kind == GenBus.Kind.OOM) item { OomCard() }
        if (gen.kind == GenBus.Kind.ERROR) item { ErrorCard(gen.error ?: "unknown") }
        item { ResultCard(vm.result.collectAsState().value) }
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
            Icon(
                Icons.Outlined.PhotoLibrary, null, Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text("No creations yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Generated images land here with full parameters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
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
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(10.dp)) {
                    val bmp = remember(rec.outPath) {
                        try {
                            val o = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                            android.graphics.BitmapFactory.decodeFile(rec.outPath, o)
                        } catch (e: Exception) { null }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(), contentDescription = null,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        rec.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${rec.mode} · ${rec.width}×${rec.height} · ${rec.steps}st · seed ${rec.seed}\n" +
                            "${fmt.format(Date(rec.createdAt))} · ${rec.durationMs / 1000}s",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                    )
                    Row {
                        TextButton(onClick = { confirmDelete = rec }) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(14.dp)); Text("  Delete")
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this creation?") },
            text = { Text(rec.prompt) },
            confirmButton = {
                TextButton(onClick = { vm.deleteRecord(rec); confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
fun InspoTab() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val groups = listOf(
            "Text → Image" to Inspo.byKind(Inspo.Kind.T2I),
            "RGBA Stickers" to Inspo.byKind(Inspo.Kind.STICKER),
            "Photo Edits" to Inspo.byKind(Inspo.Kind.EDIT),
            "Typography Posters" to Inspo.byKind(Inspo.Kind.POSTER),
        )
        groups.forEach { (title, cards) ->
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                }
            }
            items(cards) { card ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { /* copy prompt — wired via clipboard in v1.1 */ },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AutoAwesome, null, Modifier.size(14.dp),
                                tint = AppleTokens.Violet,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                card.title, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        if (card.hint.isNotEmpty()) {
                            Text(
                                card.hint, style = MaterialTheme.typography.bodySmall,
                                color = AppleTokens.Violet,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            card.prompt, maxLines = 4, overflow = TextOverflow.Ellipsis,
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
fun PromptField(value: String, onChange: (String) -> Unit, hint: String) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        placeholder = { Text(hint, style = MaterialTheme.typography.bodyMedium) },
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
fun GenerateButton(enabled: Boolean, running: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppleTokens.ActionBlue),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (running) {
            CircularProgressIndicator(
                Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text("Generating — see notification…", fontWeight = FontWeight.Medium)
        } else {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate on-device", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OomCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x14FF9500)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, Modifier.size(18.dp), tint = AppleTokens.Orange)
                Spacer(Modifier.width(8.dp))
                Text("Out of memory — recoverable", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Buffers were freed; nothing is broken. Close other apps and retry, or drop to a smaller tier (Fast/Tiny).",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun ErrorCard(msg: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x14D70015)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Generation failed", style = MaterialTheme.typography.titleMedium, color = AppleTokens.Red)
            Spacer(Modifier.height(6.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}
