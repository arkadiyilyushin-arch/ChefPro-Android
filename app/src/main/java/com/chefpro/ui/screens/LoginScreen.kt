package com.chefpro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chefpro.model.Employee
import com.chefpro.ui.components.BigActionButton
import com.chefpro.ui.components.IconCircle
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.theme.CardShape
import com.chefpro.ui.theme.ChefGradients
import com.chefpro.ui.theme.ChefOrange
import com.chefpro.ui.viewmodel.ChefProViewModel

@Composable
fun LoginScreen(
    viewModel: ChefProViewModel,
    onLoggedIn: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) ChefGradients.loginBackgroundDark() else ChefGradients.loginBackground()

    var selectedEmployeeId by remember(state.employees) {
        mutableStateOf(state.employees.firstOrNull()?.id)
    }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush),
    ) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                IconCircle(
                    icon = Icons.Default.Restaurant,
                    tint = Color.White,
                    background = ChefOrange,
                    size = 72,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = state.restaurantName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = strings.selectEmployee,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.employees, key = { it.id }) { employee ->
                        EmployeeChip(
                            employee = employee,
                            selected = employee.id == selectedEmployeeId,
                            onClick = {
                                selectedEmployeeId = employee.id
                                pin = ""
                                error = false
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = strings.enterPin,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(16.dp))

                PinDots(pinLength = pin.length, maxLength = 4, hasError = error)
                if (error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.wrongPin,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                PinKeypad(
                    onDigit = { digit ->
                        if (pin.length < 4) {
                            pin += digit
                            error = false
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            error = false
                        }
                    },
                )
                Spacer(modifier = Modifier.height(20.dp))

                BigActionButton(
                    label = strings.login,
                    icon = Icons.Default.Restaurant,
                    containerColor = ChefOrange,
                    enabled = selectedEmployeeId != null && pin.length == 4,
                    onClick = {
                        val employeeId = selectedEmployeeId
                        val employee = state.employees.find { it.id == employeeId }
                        if (employee == null || pin.length < 4) {
                            error = true
                            return@BigActionButton
                        }
                        if (viewModel.login(employee, pin)) {
                            onLoggedIn()
                        } else {
                            error = true
                            pin = ""
                        }
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EmployeeChip(
    employee: Employee,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (selected) ChefOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Card(
        modifier = Modifier
            .shadow(if (selected) 6.dp else 2.dp, CardShape, spotColor = ChefOrange.copy(alpha = 0.25f))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                ChefOrange.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        shape = CardShape,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = accent,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = employee.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) ChefOrange else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = employee.position,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PinDots(pinLength: Int, maxLength: Int, hasError: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxLength) { index ->
            val filled = index < pinLength
            Box(
                modifier = Modifier
                    .size(if (filled) 18.dp else 14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasError -> MaterialTheme.colorScheme.error
                            filled -> ChefOrange
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        },
                    )
                    .then(
                        if (filled && !hasError) {
                            Modifier.shadow(4.dp, CircleShape, spotColor = ChefOrange.copy(alpha = 0.4f))
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(72.dp))
                        "⌫" -> IconButton(
                            onClick = onBackspace,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = null, tint = ChefOrange)
                        }
                        else -> Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(2.dp, CircleShape)
                                .clickable { onDigit(key) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
