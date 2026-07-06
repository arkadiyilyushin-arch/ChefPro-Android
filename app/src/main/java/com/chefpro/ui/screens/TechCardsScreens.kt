package com.chefpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chefpro.model.ALL_ALLERGENS
import com.chefpro.model.CookingStep
import com.chefpro.model.Dish
import com.chefpro.model.DishMenuStatus
import com.chefpro.model.DishType
import com.chefpro.model.RecipeIngredient
import com.chefpro.model.RecipeVersion
import com.chefpro.model.newId
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.PermissionGate
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.components.StatusBadge
import com.chefpro.ui.localization.localized
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.theme.ErrorRed
import com.chefpro.ui.theme.SuccessGreen
import com.chefpro.ui.theme.WarningAmber
import com.chefpro.ui.viewmodel.ChefProViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TECH_CARDS_PERMISSION = "Техкарты"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechCardsListScreen(
    viewModel: ChefProViewModel,
    onDishClick: (String) -> Unit,
    onAddDish: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val currencyFormat = remember { java.text.NumberFormat.getCurrencyInstance(java.util.Locale("ru", "RU")) }

    PermissionGate(permission = TECH_CARDS_PERMISSION, viewModel = viewModel) {
        Scaffold(
            topBar = { TopAppBar(title = { Text(strings.techCards) }) },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddDish) {
                    Icon(Icons.Default.Add, contentDescription = strings.addDish)
                }
            },
        ) { padding ->
            if (state.dishes.isEmpty()) {
                EmptyStateView(
                    message = strings.noDishes,
                    icon = Icons.Default.MenuBook,
                    actionLabel = strings.addDish,
                    onAction = onAddDish,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.dishes, key = { it.id }) { dish ->
                        val cost = viewModel.calculateDishCost(dish)
                        val fc = viewModel.foodCostPercent(dish)
                        val fcColor = when {
                            dish.salePrice <= 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            fc > state.foodCostThreshold -> ErrorRed
                            else -> SuccessGreen
                        }
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDishClick(dish.id) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dish.isFavorite) {
                                            Icon(
                                                Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = ErrorRed,
                                                modifier = Modifier.padding(end = 4.dp),
                                            )
                                        }
                                        Text(
                                            text = dish.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Text(
                                        text = dish.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "${currencyFormat.format(dish.salePrice)} · ${strings.cost}: ${currencyFormat.format(cost)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (dish.salePrice > 0) {
                                    StatusBadge(
                                        text = "%.0f%%".format(fc),
                                        color = fcColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    viewModel: ChefProViewModel,
    dishId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onRecipeVersions: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val dish = state.dishes.find { it.id == dishId }
    val currencyFormat = remember { java.text.NumberFormat.getCurrencyInstance(java.util.Locale("ru", "RU")) }

    if (dish == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.techCards) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                        }
                    },
                )
            },
        ) { padding ->
            EmptyStateView(
                message = strings.noDishes,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
        return
    }

    val cost = viewModel.calculateDishCost(dish)
    val fc = viewModel.foodCostPercent(dish)
    val unmatched = viewModel.unmatchedIngredients(dish)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dish.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(dish) }) {
                        Icon(
                            if (dish.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = strings.favorite,
                            tint = if (dish.isFavorite) ErrorRed else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { onRecipeVersions(dish.id) }) {
                        Icon(Icons.Default.History, contentDescription = strings.recipeVersions)
                    }
                    IconButton(onClick = { onEdit(dish.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.edit)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(text = dish.category, color = MaterialTheme.colorScheme.primary)
                    StatusBadge(
                        text = dish.dishType.localized(state.appLanguage),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (dish.isStopListed) StatusBadge(text = strings.stopList, color = ErrorRed)
                    if (dish.isGoListed) StatusBadge(text = strings.goList, color = SuccessGreen)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile(strings.salePrice, currencyFormat.format(dish.salePrice))
                    InfoTile(strings.cost, currencyFormat.format(cost))
                    if (dish.salePrice > 0) {
                        InfoTile(strings.foodCost, "%.1f%%".format(fc))
                        InfoTile(strings.margin, currencyFormat.format(dish.salePrice - cost))
                    }
                }
            }

            if (unmatched.isNotEmpty()) {
                item {
                    Text(
                        text = "${strings.unmatchedIngredients}: ${unmatched.joinToString()}",
                        color = WarningAmber,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item { SectionTitle(title = strings.ingredients) }
            items(dish.ingredients, key = { it.id }) { ing ->
                Text(
                    text = "• ${ing.productName} — ${ing.quantity} ${ing.unit}" +
                        if (ing.yieldFactor != 1.0) " (${strings.yieldFactor}: ${ing.yieldFactor})" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (dish.steps.isNotEmpty()) {
                item { SectionTitle(title = strings.steps) }
                items(dish.steps.sortedBy { it.stepNumber }, key = { it.id }) { step ->
                    Text(
                        text = "${step.stepNumber}. ${step.instruction}" +
                            if (step.durationMinutes > 0) " (${step.durationMinutes} ${strings.durationMinutes})" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (step.tip.isNotBlank()) {
                        Text(
                            text = "💡 ${step.tip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (dish.allergens.isNotEmpty()) {
                item { SectionTitle(title = strings.allergens) }
                item {
                    Text(text = dish.allergens.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (dish.calories > 0 || dish.proteins > 0) {
                item { SectionTitle(title = strings.nutrition) }
                item {
                    Text(
                        text = "${strings.calories}: ${dish.calories} · ${strings.proteins}: ${dish.proteins}g · " +
                            "${strings.fats}: ${dish.fats}g · ${strings.carbs}: ${dish.carbs}g",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.delete) },
            text = { Text(dish.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDish(dish)
                    showDeleteDialog = false
                    onBack()
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(strings.cancel) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DishEditScreen(
    viewModel: ChefProViewModel,
    dishId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val existing = dishId?.let { id -> state.dishes.find { it.id == id } }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "") }
    var salePrice by remember(existing) { mutableStateOf(existing?.salePrice?.toString() ?: "") }
    var cookTime by remember(existing) { mutableStateOf(existing?.cookTime?.toString() ?: "") }
    var portionWeight by remember(existing) { mutableStateOf(existing?.portionWeight?.toString() ?: "") }
    var portionWeightUnit by remember(existing) { mutableStateOf(existing?.portionWeightUnit ?: "г") }
    var calories by remember(existing) { mutableStateOf(existing?.calories?.toString() ?: "") }
    var proteins by remember(existing) { mutableStateOf(existing?.proteins?.toString() ?: "") }
    var fats by remember(existing) { mutableStateOf(existing?.fats?.toString() ?: "") }
    var carbs by remember(existing) { mutableStateOf(existing?.carbs?.toString() ?: "") }
    var dishType by remember(existing) { mutableStateOf(existing?.dishType ?: DishType.DISH) }
    var menuStatus by remember(existing) { mutableStateOf(existing?.menuStatus ?: DishMenuStatus.ACTIVE) }

    val ingredients = remember(existing) {
        mutableStateListOf<RecipeIngredient>().apply {
            addAll(existing?.ingredients ?: emptyList())
        }
    }
    val steps = remember(existing) {
        mutableStateListOf<CookingStep>().apply {
            addAll(existing?.steps ?: emptyList())
        }
    }
    val selectedAllergens = remember(existing) {
        mutableStateListOf<String>().apply {
            addAll(existing?.allergens ?: emptyList())
        }
    }

    var showIngredientDialog by remember { mutableStateOf(false) }
    var showStepDialog by remember { mutableStateOf(false) }
    var editingIngredientIndex by remember { mutableStateOf<Int?>(null) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) strings.edit else strings.addDish) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val dish = Dish(
                            id = existing?.id ?: newId(),
                            name = name.trim(),
                            category = category.trim(),
                            salePrice = salePrice.toDoubleOrNull() ?: 0.0,
                            ingredients = ingredients.toList(),
                            allergens = selectedAllergens.toList(),
                            isFavorite = existing?.isFavorite ?: false,
                            cookTime = cookTime.toIntOrNull() ?: 0,
                            menuStatus = menuStatus,
                            steps = steps.toList(),
                            dishType = dishType,
                            portionWeight = portionWeight.toDoubleOrNull() ?: 0.0,
                            portionWeightUnit = portionWeightUnit,
                            calories = calories.toDoubleOrNull() ?: 0.0,
                            proteins = proteins.toDoubleOrNull() ?: 0.0,
                            fats = fats.toDoubleOrNull() ?: 0.0,
                            carbs = carbs.toDoubleOrNull() ?: 0.0,
                            isStopListed = existing?.isStopListed ?: false,
                            isGoListed = existing?.isGoListed ?: false,
                        )
                        if (existing != null) viewModel.updateDish(dish) else viewModel.addDish(dish)
                        onSaved(dish.id)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = strings.save)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.name) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(strings.category) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = salePrice,
                    onValueChange = { salePrice = it },
                    label = { Text(strings.salePrice) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = cookTime,
                    onValueChange = { cookTime = it },
                    label = { Text(strings.cookTime) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

            EnumDropdown(
                label = strings.dishType,
                options = DishType.entries,
                selected = dishType,
                labelFor = { it.localized(state.appLanguage) },
                onSelected = { dishType = it },
            )
            EnumDropdown(
                label = strings.menuStatus,
                options = DishMenuStatus.entries,
                selected = menuStatus,
                labelFor = { it.localized(state.appLanguage) },
                onSelected = { menuStatus = it },
            )

            SectionTitle(title = strings.ingredients)
            ingredients.forEachIndexed { index, ing ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingIngredientIndex = index
                            showIngredientDialog = true
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${ing.productName} — ${ing.quantity} ${ing.unit}")
                    IconButton(onClick = { ingredients.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete)
                    }
                }
            }
            TextButton(onClick = {
                editingIngredientIndex = null
                showIngredientDialog = true
            }) { Text(strings.addIngredient) }

            SectionTitle(title = strings.steps)
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingStepIndex = index
                            showStepDialog = true
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${step.stepNumber}. ${step.instruction}")
                    IconButton(onClick = { steps.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete)
                    }
                }
            }
            TextButton(onClick = {
                editingStepIndex = null
                showStepDialog = true
            }) { Text(strings.addStep) }

            SectionTitle(title = strings.allergens)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ALL_ALLERGENS.forEach { allergen ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allergen in selectedAllergens,
                            onCheckedChange = { checked ->
                                if (checked) selectedAllergens.add(allergen)
                                else selectedAllergens.remove(allergen)
                            },
                        )
                        Text(allergen, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            SectionTitle(title = strings.nutrition)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text(strings.calories) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = proteins,
                    onValueChange = { proteins = it },
                    label = { Text(strings.proteins) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fats,
                    onValueChange = { fats = it },
                    label = { Text(strings.fats) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text(strings.carbs) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = portionWeight,
                    onValueChange = { portionWeight = it },
                    label = { Text(strings.portionWeight) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = portionWeightUnit,
                    onValueChange = { portionWeightUnit = it },
                    label = { Text(strings.unit) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showIngredientDialog) {
        IngredientDialog(
            ingredient = editingIngredientIndex?.let { ingredients.getOrNull(it) },
            inventoryNames = state.inventoryItems.map { it.name },
            onDismiss = { showIngredientDialog = false },
            onSave = { ing ->
                val idx = editingIngredientIndex
                if (idx != null) ingredients[idx] = ing else ingredients.add(ing)
                showIngredientDialog = false
            },
        )
    }

    if (showStepDialog) {
        StepDialog(
            step = editingStepIndex?.let { steps.getOrNull(it) },
            nextNumber = (steps.maxOfOrNull { it.stepNumber } ?: 0) + 1,
            onDismiss = { showStepDialog = false },
            onSave = { step ->
                val idx = editingStepIndex
                if (idx != null) steps[idx] = step else steps.add(step)
                showStepDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeVersionsScreen(
    viewModel: ChefProViewModel,
    dishId: String,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val dish = state.dishes.find { it.id == dishId }
    val versions = viewModel.versionsForDish(dishId)
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val currencyFormat = remember { java.text.NumberFormat.getCurrencyInstance(java.util.Locale("ru", "RU")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.recipeVersions) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    if (dish != null) {
                        IconButton(onClick = { viewModel.saveRecipeVersion(dish) }) {
                            Icon(Icons.Default.Save, contentDescription = strings.saveVersion)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (versions.isEmpty()) {
            EmptyStateView(
                message = strings.recipeVersions,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(versions, key = { it.id }) { version ->
                    VersionCard(version, dateFormat, currencyFormat, strings)
                }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VersionCard(
    version: RecipeVersion,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    strings: com.chefpro.ui.localization.AppStrings,
) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateFormat.format(Date(version.savedAt)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = version.savedBy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${strings.salePrice}: ${currencyFormat.format(version.salePrice)}")
            Text("${strings.ingredients}: ${version.ingredients.size}")
            Text("${strings.steps}: ${version.steps.size}")
            if (version.notes.isNotBlank()) {
                Text(version.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = labelFor(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelFor(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun IngredientDialog(
    ingredient: RecipeIngredient?,
    inventoryNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (RecipeIngredient) -> Unit,
) {
    val strings = LocalAppStrings.current
    var productName by remember(ingredient) { mutableStateOf(ingredient?.productName ?: "") }
    var quantity by remember(ingredient) { mutableStateOf(ingredient?.quantity?.toString() ?: "") }
    var unit by remember(ingredient) { mutableStateOf(ingredient?.unit ?: "г") }
    var yieldFactor by remember(ingredient) { mutableStateOf(ingredient?.yieldFactor?.toString() ?: "1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ingredient != null) strings.edit else strings.addIngredient) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text(strings.productName) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (inventoryNames.isNotEmpty()) {
                    Text(
                        text = inventoryNames.take(5).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(strings.quantity) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(strings.unit) },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = yieldFactor,
                    onValueChange = { yieldFactor = it },
                    label = { Text(strings.yieldFactor) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    RecipeIngredient(
                        id = ingredient?.id ?: newId(),
                        productName = productName.trim(),
                        quantity = quantity.toDoubleOrNull() ?: 0.0,
                        unit = unit.trim(),
                        yieldFactor = yieldFactor.toDoubleOrNull() ?: 1.0,
                    ),
                )
            }) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

@Composable
private fun StepDialog(
    step: CookingStep?,
    nextNumber: Int,
    onDismiss: () -> Unit,
    onSave: (CookingStep) -> Unit,
) {
    val strings = LocalAppStrings.current
    var instruction by remember(step) { mutableStateOf(step?.instruction ?: "") }
    var duration by remember(step) { mutableStateOf(step?.durationMinutes?.toString() ?: "0") }
    var tip by remember(step) { mutableStateOf(step?.tip ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step != null) strings.edit else strings.addStep) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text(strings.instruction) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text(strings.durationMinutes) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = tip,
                    onValueChange = { tip = it },
                    label = { Text(strings.stepTip) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    CookingStep(
                        id = step?.id ?: newId(),
                        stepNumber = step?.stepNumber ?: nextNumber,
                        instruction = instruction.trim(),
                        durationMinutes = duration.toIntOrNull() ?: 0,
                        tip = tip.trim(),
                    ),
                )
            }) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
