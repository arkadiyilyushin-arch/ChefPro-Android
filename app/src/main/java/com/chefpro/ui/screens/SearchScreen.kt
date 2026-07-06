package com.chefpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.viewmodel.ChefProViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ChefProViewModel,
    onDishClick: (String) -> Unit = {},
    onInventoryClick: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("ru", "RU")) }

    val dishResults = remember(query, state.dishes) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            state.dishes.filter {
                it.name.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.ingredients.any { ing -> ing.productName.lowercase().contains(q) }
            }
        }
    }
    val inventoryResults = remember(query, state.inventoryItems) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            state.inventoryItems.filter {
                it.name.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.barcode.contains(q)
            }
        }
    }
    val hasResults = query.isNotBlank() && (dishResults.isNotEmpty() || inventoryResults.isNotEmpty())
    val noResults = query.isNotBlank() && dishResults.isEmpty() && inventoryResults.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.search) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { active = false },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text(strings.searchHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                LazyColumn {
                    if (dishResults.isNotEmpty()) {
                        item { SectionTitle(title = strings.dishes) }
                        items(dishResults, key = { "d-${it.id}" }) { dish ->
                            SearchResultRow(
                                title = dish.name,
                                subtitle = "${dish.category} · ${currencyFormat.format(dish.salePrice)}",
                                icon = Icons.Default.MenuBook,
                                onClick = {
                                    active = false
                                    onDishClick(dish.id)
                                },
                            )
                        }
                    }
                    if (inventoryResults.isNotEmpty()) {
                        item { SectionTitle(title = strings.inventory) }
                        items(inventoryResults, key = { "i-${it.id}" }) { item ->
                            SearchResultRow(
                                title = item.name,
                                subtitle = "${item.quantity} ${item.unit} · ${item.category}",
                                icon = Icons.Default.Inventory2,
                                onClick = {
                                    active = false
                                    onInventoryClick(item.id)
                                },
                            )
                        }
                    }
                }
            }

            when {
                query.isBlank() -> {
                    EmptyStateView(
                        message = strings.searchHint,
                        icon = Icons.Default.Search,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                noResults -> {
                    EmptyStateView(
                        message = strings.noResults,
                        icon = Icons.Default.Search,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                hasResults && !active -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (dishResults.isNotEmpty()) {
                            item { SectionTitle(title = strings.dishes) }
                            items(dishResults, key = { "d-${it.id}" }) { dish ->
                                SearchResultRow(
                                    title = dish.name,
                                    subtitle = "${dish.category} · ${currencyFormat.format(dish.salePrice)}",
                                    icon = Icons.Default.MenuBook,
                                    onClick = { onDishClick(dish.id) },
                                )
                            }
                        }
                        if (inventoryResults.isNotEmpty()) {
                            item { SectionTitle(title = strings.inventory) }
                            items(inventoryResults, key = { "i-${it.id}" }) { item ->
                                SearchResultRow(
                                    title = item.name,
                                    subtitle = "${item.quantity} ${item.unit} · ${item.category}",
                                    icon = Icons.Default.Inventory2,
                                    onClick = { onInventoryClick(item.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
