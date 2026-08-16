package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SyncHealthStatus
import com.example.data.model.SyncLog
import com.example.data.model.SyncLogLevel
import com.example.ui.theme.SoftPeachBackground
import com.example.ui.theme.SoftPeachBorder
import com.example.ui.theme.SoftPeachCard
import com.example.ui.theme.SoftRosePrimary
import com.example.ui.theme.SoftTextPrimary
import com.example.ui.theme.SoftTextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyncLogViewerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by viewModel.syncLogsState.collectAsState()
    val health by viewModel.syncHealthState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var isRunningTest by remember { mutableStateOf(false) }
    val testResults = remember { mutableStateListOf<Pair<String, Boolean>>() }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "PHOTO" -> logs.filter { it.category == "PHOTO_SYNC" }
            "FEED" -> logs.filter { it.category == "FEED_PROPAGATION" || it.isDeliveredToOthers }
            "ERROR" -> logs.filter { it.level == "ERROR" || it.level == "WARNING" }
            "CLOUD" -> logs.filter { it.category == "AUTH_CLOUD" || it.category == "REALTIME_STREAM" }
            else -> logs
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, SoftPeachBorder, RoundedCornerShape(24.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (health.totalErrorsCount > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (health.totalErrorsCount > 0) Icons.Default.Warning else Icons.Default.CloudDone,
                                contentDescription = "Sync Hub",
                                tint = if (health.totalErrorsCount > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Central de Logs & Sincronização",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = SoftTextPrimary
                            )
                            val isSupabaseOk = com.example.service.SupabaseAuthHelper.isConfigured(context)
                            Text(
                                text = if (isSupabaseOk) {
                                    if (health.isGuestUser) "Modo Convidado • Supabase Conectado" else "Usuário Autenticado • Supabase Ativo"
                                } else {
                                    "Modo Local • Chaves Supabase Pendentes"
                                },
                                fontSize = 12.sp,
                                color = if (isSupabaseOk) SoftTextSecondary else Color(0xFFE65100)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_sync_dialog")) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SoftTextSecondary)
                    }
                }

                // Banner when Supabase credentials are not yet configured
                var showConfigFields by remember { mutableStateOf(false) }
                var inputUrl by remember { mutableStateOf(com.example.service.SupabaseAuthHelper.getSupabaseUrl(context).takeIf { !it.contains("placeholder") && !it.contains("your-project") } ?: "") }
                var inputKey by remember { mutableStateOf(com.example.service.SupabaseAuthHelper.getSupabaseAnonKey(context).takeIf { !it.contains("placeholder") && !it.contains("your-anon-key") } ?: "") }
                val isCurrentlyConfigured = com.example.service.SupabaseAuthHelper.isConfigured(context)

                if (!isCurrentlyConfigured || showConfigFields) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isCurrentlyConfigured) Color(0xFFF1F8E9) else Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isCurrentlyConfigured) "⚡ Supabase Conectado" else "🔑 Configuração do Supabase",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isCurrentlyConfigured) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                Text(
                                    text = if (showConfigFields) "Fechar ▲" else "Configurar ▼",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftRosePrimary,
                                    modifier = Modifier.clickable { showConfigFields = !showConfigFields }
                                )
                            }
                            if (!isCurrentlyConfigured && !showConfigFields) {
                                Text(
                                    text = "Toque em 'Configurar' para inserir sua URL e Anon Key do Supabase ou adicione no painel Secrets (.env).",
                                    fontSize = 11.sp,
                                    color = Color(0xFF5D4037),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (showConfigFields) {
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.material3.OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    label = { Text("SUPABASE_URL (ex: https://xyz.supabase.co)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.material3.OutlinedTextField(
                                    value = inputKey,
                                    onValueChange = { inputKey = it },
                                    label = { Text("SUPABASE_ANON_KEY (chave pública anon)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isCurrentlyConfigured) {
                                        OutlinedButton(
                                            onClick = {
                                                com.example.service.SupabaseAuthHelper.clearCustomConfig(context)
                                                inputUrl = ""
                                                inputKey = ""
                                                Toast.makeText(context, "Configurações restauradas", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Restaurar", fontSize = 11.sp)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (inputUrl.isNotBlank() && inputKey.isNotBlank()) {
                                                com.example.service.SupabaseAuthHelper.saveCustomConfig(context, inputUrl, inputKey)
                                                Toast.makeText(context, "Supabase configurado com sucesso! Sincronizando...", Toast.LENGTH_SHORT).show()
                                                showConfigFields = false
                                                viewModel.forceSyncAll()
                                            } else {
                                                Toast.makeText(context, "Preencha a URL e a Chave Anon!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftRosePrimary)
                                    ) {
                                        Text("Salvar & Conectar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Summary Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Status da Foto
                    StatusMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PhotoCamera,
                        title = "Sinc. Foto",
                        value = if (health.lastPhotoSyncSuccess) "Ativa" else "Falha",
                        subValue = "${health.totalPostsSynced} posts",
                        color = if (health.lastPhotoSyncSuccess) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )

                    // Card 2: Entrega no Feed
                    StatusMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Diversity3,
                        title = "Outros Usuários",
                        value = "Recebido",
                        subValue = "${health.totalOtherUsersPostsReceived} recebidos",
                        color = Color(0xFF1976D2)
                    )

                    // Card 3: Erros
                    StatusMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = if (health.totalErrorsCount > 0) Icons.Default.Error else Icons.Default.CheckCircle,
                        title = "Erros Nuvem",
                        value = "${health.totalErrorsCount} erros",
                        subValue = if (health.totalErrorsCount == 0) "100% Saudável" else "Verifique logs",
                        color = if (health.totalErrorsCount > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Bar: Test, Force Sync, Copy, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isRunningTest) {
                                isRunningTest = true
                                testResults.clear()
                                scope.launch {
                                    try {
                                        viewModel.runSyncDiagnosticTest().collect { step ->
                                            testResults.add(step)
                                        }
                                    } catch (e: Exception) {
                                        testResults.add(Pair("❌ Erro inesperado: ${e.localizedMessage ?: e.javaClass.simpleName}", false))
                                    } finally {
                                        isRunningTest = false
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("btn_run_diag_test")
                    ) {
                        if (isRunningTest) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testando...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Diagnóstico", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.forceSyncAll() },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("btn_force_sync_dialog")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sincronizar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = {
                            val text = viewModel.exportSyncLogs()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Logs de Sincronização", text)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Logs copiados para a área de transferência! 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp).testTag("btn_copy_logs")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar logs", tint = SoftRosePrimary)
                    }

                    IconButton(
                        onClick = { viewModel.clearSyncLogs() },
                        modifier = Modifier.size(36.dp).testTag("btn_clear_logs")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Limpar logs", tint = Color.Gray)
                    }
                }

                // Diagnostic Test Running Results Box
                AnimatedVisibility(visible = testResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftPeachBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Resultado do Diagnóstico E2E:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (!isRunningTest) {
                                    Text("Concluído", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            testResults.forEach { result ->
                                Text(
                                    text = result.first,
                                    fontSize = 11.sp,
                                    color = if (result.second) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Todos (${logs.size})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftPeachCard,
                            selectedLabelColor = SoftRosePrimary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "PHOTO",
                        onClick = { selectedFilter = "PHOTO" },
                        label = { Text("📸 Fotos", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftPeachCard,
                            selectedLabelColor = SoftRosePrimary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "FEED",
                        onClick = { selectedFilter = "FEED" },
                        label = { Text("👥 Feed & Outros", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftPeachCard,
                            selectedLabelColor = SoftRosePrimary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "ERROR",
                        onClick = { selectedFilter = "ERROR" },
                        label = { Text("❌ Erros (${health.totalErrorsCount})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFD32F2F)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "CLOUD",
                        onClick = { selectedFilter = "CLOUD" },
                        label = { Text("☁️ Nuvem", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftPeachCard,
                            selectedLabelColor = SoftRosePrimary
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SoftPeachBorder
                )

                // Log List
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum log encontrado nesta categoria.",
                                fontSize = 13.sp,
                                color = SoftTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { logItem ->
                            SyncLogItemCard(
                                log = logItem,
                                onRetry = { logItem.postId?.let { viewModel.retryPostSync(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subValue: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SoftPeachBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 10.5.sp, color = SoftTextSecondary, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            Text(subValue, fontSize = 10.sp, color = SoftTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SyncLogItemCard(
    log: SyncLog,
    onRetry: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val levelColor = when (log.level) {
        "ERROR" -> Color(0xFFD32F2F)
        "WARNING" -> Color(0xFFF57C00)
        "SUCCESS" -> Color(0xFF2E7D32)
        else -> Color(0xFF1976D2)
    }

    val levelBg = when (log.level) {
        "ERROR" -> Color(0xFFFFEBEE)
        "WARNING" -> Color(0xFFFFF3E0)
        "SUCCESS" -> Color(0xFFE8F5E9)
        else -> Color(0xFFE3F2FD)
    }

    val icon = when (log.category) {
        "PHOTO_SYNC" -> Icons.Default.PhotoCamera
        "FEED_PROPAGATION" -> Icons.Default.Diversity3
        "REALTIME_STREAM" -> Icons.Default.CloudSync
        "AUTH_CLOUD" -> Icons.Default.CloudDone
        "ERROR_DIAGNOSTIC" -> Icons.Default.Error
        else -> Icons.Default.Info
    }

    val timeFormatted = remember(log.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = levelBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = levelColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = log.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = levelColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFormatted,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.message,
                fontSize = 11.5.sp,
                color = SoftTextPrimary,
                lineHeight = 15.sp
            )

            if (log.isDeliveredToOthers) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF2E7D32), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Visível para outros usuários no feed da comunidade",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (log.postId != null) {
                        Text(
                            text = "📌 Post ID: #${log.postId}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (log.photoUri != null) {
                        Text(
                            text = "📸 Foto Local: ${log.photoUri}",
                            fontSize = 10.5.sp,
                            color = SoftTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (log.errorCode != null) {
                        Text(
                            text = "⚠️ Código de Erro: ${log.errorCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                    if (!log.technicalDetails.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.technicalDetails,
                            fontSize = 10.5.sp,
                            color = Color(0xFF37474F),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }

                    if (log.level == "ERROR" && log.postId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = SoftRosePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Reenviar Foto para Nuvem", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
