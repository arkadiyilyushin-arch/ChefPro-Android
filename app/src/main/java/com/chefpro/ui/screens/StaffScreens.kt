package com.chefpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chefpro.model.ChecklistType
import com.chefpro.model.Employee
import com.chefpro.model.ModelHelpers.duration
import com.chefpro.model.ModelHelpers.isCritical
import com.chefpro.model.ModelHelpers.isOk
import com.chefpro.model.ModelHelpers.statusLabel
import com.chefpro.model.TemperatureLog
import com.chefpro.model.WorkShift
import com.chefpro.ui.components.BigActionButton
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.PermissionGate
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    PermissionGate(permission = "Настройки", hasPermission = viewModel::hasPermission) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Сотрудники") }) },
            floatingActionButton = {
                Button(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Добавить")
                }
            },
        ) { padding ->
            if (state.employees.isEmpty()) {
                EmptyStateView(Icons.Default.People, "Нет сотрудников", "Добавьте первого сотрудника", Modifier.padding(padding).fillMaxSize())
            } else {
                LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.employees, key = { it.id }) { employee ->
                        BigCard(Modifier.padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(employee.name, fontWeight = FontWeight.Bold)
                                    Text(employee.position, style = MaterialTheme.typography.bodySmall)
                                    Text(employee.phone, style = MaterialTheme.typography.labelSmall)
                                    Text(employee.permissions.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteEmployee(employee) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Новый сотрудник") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(position, { position = it }, label = { Text("Должность") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pin, { pin = it }, label = { Text("PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank() || pin.length < 4) return@TextButton
                    viewModel.addEmployee(
                        Employee(
                            name = name.trim(),
                            position = position.ifBlank { "Сотрудник" },
                            phone = phone.trim(),
                            pin = pin,
                            permissions = listOf("Склад", "Списания"),
                        ),
                    )
                    showDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkScheduleView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedEmployeeId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("График работы") }) },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Смена")
            }
        },
    ) { padding ->
        if (state.workSchedule.isEmpty()) {
            EmptyStateView(Icons.Default.Schedule, "График пуст", "Добавьте рабочие смены", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.workSchedule.sortedByDescending { it.date }, key = { it.id }) { shift ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(shift.employeeName, fontWeight = FontWeight.Bold)
                                Text(formatDate(shift.date), style = MaterialTheme.typography.bodySmall)
                                Text("${formatTime(shift.startTime)} — ${formatTime(shift.endTime)} • ${shift.duration()}", color = ChefAccent)
                                if (shift.notes.isNotBlank()) Text(shift.notes, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.deleteWorkShift(shift) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val now = System.currentTimeMillis()
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Добавить смену") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.employees.forEach { emp ->
                        FilterChip(selected = selectedEmployeeId == emp.id, onClick = { selectedEmployeeId = emp.id }, label = { Text(emp.name) })
                    }
                    OutlinedTextField(notes, { notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val emp = state.employees.firstOrNull { it.id == selectedEmployeeId } ?: return@TextButton
                    viewModel.addWorkShift(
                        WorkShift(
                            employeeID = emp.id,
                            employeeName = emp.name,
                            date = now,
                            startTime = now,
                            endTime = now + 8 * 60 * 60 * 1000L,
                            notes = notes.trim(),
                        ),
                    )
                    showDialog = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var selectedType by remember { mutableStateOf(ChecklistType.OPENING) }
    val items = state.checklists.filter { it.type == selectedType }
    val done = items.count { it.isCompleted }

    Scaffold(topBar = { TopAppBar(title = { Text("Чеклист смены") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedType == ChecklistType.OPENING, onClick = { selectedType = ChecklistType.OPENING }, label = { Text("Открытие") })
                FilterChip(selected = selectedType == ChecklistType.CLOSING, onClick = { selectedType = ChecklistType.CLOSING }, label = { Text("Закрытие") })
            }
            Text("Выполнено: $done / ${items.size}", fontWeight = FontWeight.Bold, color = ChefAccent)
            items.forEach { item ->
                BigCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.isCompleted, onCheckedChange = { checked ->
                            if (checked && !item.isCompleted) viewModel.completeChecklist(item, state.profile.name)
                        })
                        Column(Modifier.weight(1f)) {
                            Text(item.text, fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Bold)
                            if (item.isCompleted) {
                                Text("✓ ${item.completedBy} • ${item.completedAt?.let { formatTime(it) } ?: ""}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
            BigActionButton("Сбросить чеклист", Icons.Default.CheckCircle, onClick = { viewModel.resetChecklists(selectedType) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureLogView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("Холодильник №1") }
    var temperature by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Температурный журнал") }) },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Запись")
            }
        },
    ) { padding ->
        if (state.temperatureLogs.isEmpty()) {
            EmptyStateView(Icons.Default.Thermostat, "Записей нет", "Добавьте первую температуру", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.temperatureLogs, key = { it.id }) { log ->
                    val color = when {
                        log.isCritical() -> Color.Red
                        log.isOk() -> Color(0xFF2E7D32)
                        else -> ChefAccent
                    }
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(log.location, fontWeight = FontWeight.Bold)
                                Text("${log.temperature}°C • ${log.statusLabel()}", color = color, fontWeight = FontWeight.Bold)
                                Text("${log.recordedBy} • ${formatDateTime(log.recordedAt)}", style = MaterialTheme.typography.labelSmall)
                                if (log.notes.isNotBlank()) Text(log.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteTemperatureLog(log) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Новая запись") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Холодильник №1", "Морозильник", "Горячий цех").forEach { loc ->
                        FilterChip(selected = location == loc, onClick = { location = loc }, label = { Text(loc) })
                    }
                    OutlinedTextField(temperature, { temperature = it }, label = { Text("°C") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(notes, { notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val temp = temperature.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                    viewModel.addTemperatureLog(
                        TemperatureLog(
                            location = location,
                            temperature = temp,
                            recordedBy = state.profile.name,
                            notes = notes.trim(),
                        ),
                    )
                    showDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}
