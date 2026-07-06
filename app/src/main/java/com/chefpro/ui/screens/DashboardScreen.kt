package com.chefpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefpro.model.ModelHelpers.isExpired
import com.chefpro.model.ModelHelpers.isExpiringSoon
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.InfoCard
import com.chefpro.ui.components.OfflineStatusBanner
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.components.StatusBadge
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.theme.ErrorRed
import com.chefpro.ui.theme.SuccessGreen
import com.chefpro.ui.theme.WarningAmber
import com.chefpro.ui.viewmodel.ChefProViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ChefProViewModel,
    onNavigateToTechCards: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onAddDish: () -> Unit = {},
    onAddProduct: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()
    val strings = LocalAppStrings.current

    val lowStock by viewModel.lowStockItems.collectAsState()
    val expiring by viewModel.expiringItems.collectAsState()
    val currentEmployee by viewModel.currentEmployee.collectAsState()

    val highFoodCost = state.dishes.filter { dish ->
        dish.salePrice > 0 &&
            viewModel.hasFoodCostPercent(dish) &&
            viewModel.foodCostPercent(dish) > state.foodCostThreshold
    }
    val stockValue = state.inventoryItems.sumOf { it.quantity * it.pricePerUnit }

    val greeting = greetingForHour(strings, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    val employeeName = currentEmployee?.name ?: state.profile.name

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.restaurantName) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                OfflineStatusBanner(
                    isOffline = isOffline,
                    isSyncing = isSyncing,
                    pendingCount = pendingSync,
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = employeeName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        InfoCard(
                            title = strings.dishes,
                            value = state.dishes.size.toString(),
                            subtitle = strings.dishesCount,
                            icon = Icons.Default.MenuBook,
                            modifier = Modifier.width(140.dp),
                        )
                    }
                    item {
                        InfoCard(
                            title = strings.stockValue,
                            value = formatMoney(stockValue),
                            subtitle = "${state.inventoryItems.size} ${strings.productsCount}",
                            icon = Icons.Default.Inventory2,
                            modifier = Modifier.width(140.dp),
                        )
                    }
                    item {
                        InfoCard(
                            title = strings.lowStock,
                            value = lowStock.size.toString(),
                            icon = Icons.Default.Warning,
                            accentColor = if (lowStock.isNotEmpty()) WarningAmber else SuccessGreen,
                            modifier = Modifier.width(140.dp),
                        )
                    }
                    item {
                        val avgFc = if (state.dishes.isNotEmpty()) {
                            state.dishes
                                .filter { it.salePrice > 0 }
                                .map { viewModel.foodCostPercent(it) }
                                .average()
                        } else 0.0
                        InfoCard(
                            title = strings.foodCost,
                            value = "%.0f%%".format(avgFc),
                            subtitle = "≤ ${state.foodCostThreshold.toInt()}%",
                            icon = Icons.Default.TrendingUp,
                            accentColor = if (avgFc > state.foodCostThreshold) ErrorRed else SuccessGreen,
                            modifier = Modifier.width(140.dp),
                        )
                    }
                }
            }

            if (lowStock.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = strings.lowStock,
                        action = strings.viewAll,
                        onActionClick = onNavigateToInventory,
                    )
                }
                items(lowStock.take(5), key = { it.id }) { item ->
                    AlertRow(
                        title = item.name,
                        subtitle = "${item.quantity} ${item.unit} / min ${item.minQuantity} ${item.unit}",
                        badgeText = strings.lowStock,
                        badgeColor = WarningAmber,
                        onClick = onNavigateToInventory,
                    )
                }
            }

            if (expiring.isNotEmpty()) {
                item {
                    SectionTitle(title = strings.expiringSoon)
                }
                items(expiring.take(5), key = { "exp-${it.id}" }) { item ->
                    val badge = if (item.isExpired()) strings.expired else strings.expiringSoon
                    val color = if (item.isExpired()) ErrorRed else WarningAmber
                    AlertRow(
                        title = item.name,
                        subtitle = "${item.quantity} ${item.unit}",
                        badgeText = badge,
                        badgeColor = color,
                        onClick = onNavigateToInventory,
                    )
                }
            }

            if (highFoodCost.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = strings.foodCostWarning,
                        action = strings.viewAll,
                        onActionClick = onNavigateToTechCards,
                    )
                }
                items(highFoodCost.take(5), key = { "fc-${it.id}" }) { dish ->
                    val fc = viewModel.foodCostPercent(dish)
                    AlertRow(
                        title = dish.name,
                        subtitle = "${strings.salePrice}: ${formatMoney(dish.salePrice)}",
                        badgeText = "%.0f%% FC".format(fc),
                        badgeColor = ErrorRed,
                        onClick = onNavigateToTechCards,
                    )
                }
            }

            item {
                SectionTitle(title = strings.quickActions)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        QuickActionCard(
                            label = strings.addDish,
                            icon = Icons.Default.Add,
                            onClick = onAddDish,
                        )
                    }
                    item {
                        QuickActionCard(
                            label = strings.addProduct,
                            icon = Icons.Default.Inventory2,
                            onClick = onAddProduct,
                        )
                    }
                    item {
                        QuickActionCard(
                            label = strings.search,
                            icon = Icons.Default.Search,
                            onClick = onNavigateToSearch,
                        )
                    }
                    item {
                        QuickActionCard(
                            label = strings.techCards,
                            icon = Icons.Default.MenuBook,
                            onClick = onNavigateToTechCards,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRow(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    BigCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(text = badgeText, color = badgeColor)
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun greetingForHour(strings: com.chefpro.ui.localization.AppStrings, hour: Int): String =
    when (hour) {
        in 5..11 -> strings.greetingMorning
        in 12..16 -> strings.greetingDay
        in 17..22 -> strings.greetingEvening
        else -> strings.greetingNight
    }
