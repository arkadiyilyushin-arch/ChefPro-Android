package com.chefpro.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chefpro.model.AppColorScheme
import com.chefpro.model.AppLanguage
import com.chefpro.model.UserProfile
import com.chefpro.ui.components.BigActionButton
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.OfflineStatusBanner
import com.chefpro.ui.components.PermissionGate
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.theme.SuccessGreen
import com.chefpro.ui.viewmodel.ChefProViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var name by remember(state.profile.name) { mutableStateOf(state.profile.name) }
    var position by remember(state.profile.position) { mutableStateOf(state.profile.position) }
    var phone by remember(state.profile.phone) { mutableStateOf(state.profile.phone) }

    Scaffold(topBar = { TopAppBar(title = { Text("Профиль") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigCard {
                Text(state.profile.name.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = ChefAccent)
                Text("Разрешения: ${state.profile.permissions.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(name, { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(position, { position = it }, label = { Text("Должность") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                viewModel.updateProfile(state.profile.copy(name = name.trim(), position = position.trim(), phone = phone.trim()))
            }) { Text("Сохранить") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var fcThreshold by remember { mutableDoubleStateOf(state.foodCostThreshold) }
    var expiryDays by remember { mutableIntStateOf(state.expiryWarningDays) }

    PermissionGate(permission = "Настройки", hasPermission = viewModel::hasPermission) {
        Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
            Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle("Тема")
                AppColorScheme.entries.forEach { scheme ->
                    FilterChip(
                        selected = state.appColorScheme == scheme,
                        onClick = { viewModel.setAppColorScheme(scheme) },
                        label = { Text(enumLabelRu(scheme.name)) },
                    )
                }
                SectionTitle("Язык")
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = state.appLanguage == lang,
                        onClick = { viewModel.setAppLanguage(lang) },
                        label = { Text(enumLabelRu(lang.name)) },
                    )
                }
                SectionTitle("Уведомления")
                settingsSwitch("Уведомления", state.notificationsEnabled) { viewModel.setNotificationsEnabled(it) }
                settingsSwitch("Ежедневная сводка", state.dailyDigestEnabled) { viewModel.setDailyDigestEnabled(it) }
                settingsSwitch("HACCP напоминания", state.haccpRemindersEnabled) { viewModel.setHaccpRemindersEnabled(it) }
                BigCard {
                    Text("Порог Food Cost: ${fcThreshold.toInt()}%")
                    androidx.compose.material3.Slider(
                        value = fcThreshold.toFloat(),
                        onValueChange = { fcThreshold = it.toDouble() },
                        valueRange = 20f..50f,
                    )
                    Text("Срок годности (дней): $expiryDays")
                    androidx.compose.material3.Slider(
                        value = expiryDays.toFloat(),
                        onValueChange = { expiryDays = it.toInt() },
                        valueRange = 1f..14f,
                        steps = 12,
                    )
                    Button(onClick = {
                        viewModel.setFoodCostThreshold(fcThreshold)
                        viewModel.setExpiryWarningDays(expiryDays)
                    }) { Text("Сохранить пороги") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupView(viewModel: ChefProViewModel) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Резервная копия") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigActionButton("Экспорт JSON", Icons.Default.Backup) {
                val json = viewModel.exportBackupJson()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(intent, "Экспорт бэкапа"))
            }
            SectionTitle("Импорт JSON")
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("JSON бэкапа") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
            )
            Button(onClick = {
                if (importText.isNotBlank()) viewModel.importBackupJson(importText)
            }) { Text("Импортировать") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncView(viewModel: ChefProViewModel) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSync by viewModel.lastSyncDate.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val pending by viewModel.pendingSyncCount.collectAsState()
    val syncCode = viewModel.syncCode
    val isFirebaseAvailable = viewModel.isFirebaseAvailable
    var partnerCode by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru", "RU")) }

    Scaffold(topBar = { TopAppBar(title = { Text("Синхронизация") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OfflineStatusBanner(isOffline = isOffline, pendingCount = pending)
            BigCard {
                Text("Код синхронизации", style = MaterialTheme.typography.labelSmall)
                Text(
                    syncCode,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Передайте этот код партнёру для подключения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BigCard {
                Text(
                    if (isFirebaseAvailable) "Firebase доступен" else "Firebase недоступен",
                    fontWeight = FontWeight.Bold,
                    color = if (isFirebaseAvailable) SuccessGreen else MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = partnerCode,
                onValueChange = { partnerCode = it.uppercase().take(8) },
                label = { Text("Код партнёра") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (partnerCode.isNotBlank()) {
                        viewModel.connectToSyncCode(partnerCode)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = partnerCode.length == 8,
            ) {
                Text("Подключиться")
            }
            BigCard {
                Text(if (isSyncing) "Синхронизация…" else "Готово", fontWeight = FontWeight.Bold)
                Text("Последняя: ${lastSync?.let { dateFormat.format(Date(it)) } ?: "никогда"}", style = MaterialTheme.typography.bodySmall)
                syncError?.let { Text("Ошибка: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            BigActionButton("Синхронизировать", Icons.Default.Sync, onClick = { viewModel.syncFromCloud() })
            settingsSwitch("Офлайн режим", isOffline) { viewModel.setOfflineMode(it) }
            Text("Ожидает отправки: $pending", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultitenancyView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var restaurantName by remember(state.restaurantName) { mutableStateOf(state.restaurantName) }

    Scaffold(topBar = { TopAppBar(title = { Text("Рестораны") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Default.Store, contentDescription = null, tint = ChefAccent)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Текущий ресторан", style = MaterialTheme.typography.labelSmall)
                        Text(state.restaurantName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            OutlinedTextField(restaurantName, { restaurantName = it }, label = { Text("Название ресторана") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.setRestaurantName(restaurantName.trim()) }) { Text("Сохранить") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantSwitcherView(viewModel: ChefProViewModel) = MultitenancyView(viewModel)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineQueueView(viewModel: ChefProViewModel) {
    val isOffline by viewModel.isOffline.collectAsState()
    val pending by viewModel.pendingSyncCount.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Офлайн-очередь") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OfflineStatusBanner(isOffline = isOffline, pendingCount = pending)
            if (pending == 0) {
                com.chefpro.ui.components.EmptyStateView(Icons.Default.CloudOff, "Очередь пуста", "Все изменения синхронизированы", Modifier.fillMaxSize())
            } else {
                BigCard {
                    Text("$pending изменений ожидают синхронизации", fontWeight = FontWeight.Bold)
                    Text(if (isOffline) "Подключитесь к сети для отправки" else "Нажмите «Синхронизировать»", style = MaterialTheme.typography.bodySmall)
                }
                BigActionButton("Синхронизировать сейчас", Icons.Default.Sync, onClick = { viewModel.syncFromCloud() })
                Button(onClick = { viewModel.clearPendingSync() }) { Text("Очистить счётчик") }
            }
        }
    }
}

@Composable
private fun settingsSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
