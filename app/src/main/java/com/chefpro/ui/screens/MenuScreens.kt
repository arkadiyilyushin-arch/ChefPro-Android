package com.chefpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chefpro.domain.ChefProEngine
import com.chefpro.model.MenuCollection
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCollectionsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🍽️") }
    var selectedDishIds by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Сборники меню") }) },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Сборник")
            }
        },
    ) { padding ->
        if (state.menuCollections.isEmpty()) {
            EmptyStateView(Icons.Default.Collections, "Сборников нет", "Создайте первый сборник", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.menuCollections, key = { it.id }) { collection ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(collection.emoji, style = MaterialTheme.typography.headlineMedium)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(collection.name, fontWeight = FontWeight.Bold)
                                Text("${collection.dishIDs.size} блюд", style = MaterialTheme.typography.bodySmall)
                                collection.dishIDs.mapNotNull { id -> state.dishes.firstOrNull { it.id == id }?.name }
                                    .take(3).forEach { Text("• $it", style = MaterialTheme.typography.labelSmall) }
                            }
                            IconButton(onClick = { viewModel.deleteCollection(collection) }) {
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
            title = { Text("Новый сборник") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(emoji, { emoji = it }, label = { Text("Эмодзи") }, modifier = Modifier.fillMaxWidth())
                    SectionTitle("Блюда")
                    state.dishes.forEach { dish ->
                        FilterChip(
                            selected = selectedDishIds.contains(dish.id),
                            onClick = {
                                selectedDishIds = if (selectedDishIds.contains(dish.id)) {
                                    selectedDishIds - dish.id
                                } else {
                                    selectedDishIds + dish.id
                                }
                            },
                            label = { Text(dish.name) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) return@TextButton
                    viewModel.addCollection(MenuCollection(name = name.trim(), emoji = emoji, dishIDs = selectedDishIds.toList()))
                    showDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishGalleryView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val withPhotos = state.dishes.filter { it.photoFilename != null }
    val withoutPhotos = state.dishes.filter { it.photoFilename == null }

    Scaffold(topBar = { TopAppBar(title = { Text("Галерея блюд") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("С фото (${withPhotos.size})") }
            if (withPhotos.isEmpty()) {
                item { Text("Нет блюд с фото", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(withPhotos, key = { it.id }) { dish ->
                    galleryCard(dish, viewModel)
                }
            }
            item { SectionTitle("Без фото (${withoutPhotos.size})") }
            items(withoutPhotos, key = { it.id }) { dish ->
                galleryCard(dish, viewModel)
            }
        }
    }
}

@Composable
private fun galleryCard(dish: com.chefpro.model.Dish, viewModel: ChefProViewModel) {
    BigCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(dish.name, fontWeight = FontWeight.Bold)
                Text(dish.category, style = MaterialTheme.typography.bodySmall)
                Text("FC ${formatPercent(viewModel.foodCostPercent(dish))}", color = ChefAccent, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (dish.photoFilename != null) "📷" else "—", style = MaterialTheme.typography.headlineSmall)
                Text(formatMoney(dish.salePrice), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeTemplatesView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val templates = state.dishes.filter { it.steps.isNotEmpty() || it.ingredients.isNotEmpty() }

    Scaffold(topBar = { TopAppBar(title = { Text("Шаблоны техкарт") }) }) { padding ->
        if (templates.isEmpty()) {
            EmptyStateView(Icons.Default.PhotoLibrary, "Нет техкарт", "Добавьте блюда с ингредиентами", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates, key = { it.id }) { dish ->
                    BigCard {
                        Text(dish.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${dish.ingredients.size} ингр. • ${dish.steps.size} шагов • ${dish.cookTime} мин", style = MaterialTheme.typography.bodySmall)
                        Text("Себест.: ${formatMoney(viewModel.dishCost(dish))}", color = ChefAccent, fontWeight = FontWeight.Bold)
                        dish.ingredients.take(4).forEach { ing ->
                            Text("• ${ing.productName} ${ing.quantity} ${ing.unit}", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.saveRecipeVersion(dish, state.profile.name) }) {
                            Text("Сохранить версию")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeVersionsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Версии техкарт") }) }) { padding ->
        if (state.recipeVersions.isEmpty()) {
            EmptyStateView(Icons.Default.History, "Версий нет", "Сохраните версию из шаблона", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.recipeVersions, key = { it.id }) { version ->
                    BigCard {
                        Text(version.dishName, fontWeight = FontWeight.Bold)
                        Text("${formatDateTime(version.savedAt)} • ${version.savedBy}", style = MaterialTheme.typography.bodySmall)
                        Text("Цена: ${formatMoney(version.salePrice)} • ${version.ingredients.size} ингр.", style = MaterialTheme.typography.labelSmall)
                        if (version.notes.isNotBlank()) Text(version.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterView() {
    var quantity by remember { mutableStateOf("1000") }
    var fromUnit by remember { mutableStateOf("г") }
    var toUnit by remember { mutableStateOf("кг") }
    val qty = parsePositiveDouble(quantity) ?: 0.0
    val result = ChefProEngine.convert(qty, fromUnit, toUnit)
    val units = listOf("г", "кг", "мл", "л", "шт")

    Scaffold(topBar = { TopAppBar(title = { Text("Конвертер единиц") }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Количество") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            SectionTitle("Из")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                units.forEach { unit ->
                    FilterChip(selected = fromUnit == unit, onClick = { fromUnit = unit }, label = { Text(unit) })
                }
            }
            SectionTitle("В")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                units.forEach { unit ->
                    FilterChip(selected = toUnit == unit, onClick = { toUnit = unit }, label = { Text(unit) })
                }
            }
            BigCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ChefAccent)
                    Text("$qty $fromUnit =", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (result > 0 || qty == 0.0) "${if (result % 1.0 == 0.0) result.toLong() else String.format("%.3f", result)} $toUnit"
                        else "Конвертация невозможна",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChefAccent,
                    )
                }
            }
        }
    }
}
