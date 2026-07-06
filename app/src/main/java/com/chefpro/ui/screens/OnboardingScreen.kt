package com.chefpro.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chefpro.ui.components.IconCircle
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.theme.ButtonShape
import com.chefpro.ui.theme.ChefGradients
import com.chefpro.ui.theme.ChefOrange
import com.chefpro.ui.theme.ChefTeal
import com.chefpro.ui.viewmodel.ChefProViewModel
import kotlinx.coroutines.launch

private val pageColors = listOf(ChefOrange, ChefTeal, Color(0xFF7C4DFF), Color(0xFF00838F))

@Composable
fun OnboardingScreen(
    viewModel: ChefProViewModel,
    onComplete: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) ChefGradients.loginBackgroundDark() else ChefGradients.loginBackground()

    var restaurantName by remember { mutableStateOf(state.restaurantName) }

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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(4) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) pageColors[index] else pageColors[index].copy(alpha = 0.3f),
                                ),
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                ) { page ->
                    when (page) {
                        0 -> OnboardingPage(
                            icon = Icons.Default.Restaurant,
                            title = strings.onboardingWelcome,
                            body = strings.onboardingWelcomeBody,
                            accent = pageColors[0],
                        )
                        1 -> OnboardingPage(
                            icon = Icons.Default.MenuBook,
                            title = strings.onboardingTechCards,
                            body = strings.onboardingTechCardsBody,
                            accent = pageColors[1],
                        )
                        2 -> OnboardingPage(
                            icon = Icons.Default.Inventory2,
                            title = strings.onboardingInventory,
                            body = strings.onboardingInventoryBody,
                            accent = pageColors[2],
                        )
                        else -> Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            OnboardingPage(
                                icon = Icons.Default.Store,
                                title = strings.onboardingSetup,
                                body = strings.onboardingSetupBody,
                                accent = pageColors[3],
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = restaurantName,
                                onValueChange = { restaurantName = it },
                                label = { Text(strings.restaurantName) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pagerState.currentPage < 3) {
                        TextButton(onClick = {
                            viewModel.setHasSeenOnboarding(true)
                            onComplete()
                        }) {
                            Text(strings.skip)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (pagerState.currentPage < 3) {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = pageColors[pagerState.currentPage]),
                        ) {
                            Text(strings.next)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (restaurantName.isNotBlank()) {
                                    viewModel.updateSettings { it.copy(restaurantName = restaurantName.trim()) }
                                }
                                viewModel.setHasSeenOnboarding(true)
                                onComplete()
                            },
                            enabled = restaurantName.isNotBlank(),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ChefOrange),
                        ) {
                            Text(strings.getStarted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconCircle(
            icon = icon,
            tint = Color.White,
            background = accent,
            size = 88,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
