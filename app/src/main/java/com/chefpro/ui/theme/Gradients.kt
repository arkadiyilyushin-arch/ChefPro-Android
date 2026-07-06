package com.chefpro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ChefGradients {

    @Composable
    fun heroBrush(): Brush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF5722),
                Color(0xFFFF7043),
                Color(0xFFFF8A65),
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 600f),
        )
    }

    @Composable
    fun heroBrushDark(): Brush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFBF360C),
                Color(0xFFE64A19),
                Color(0xFFFF5722),
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 400f),
        )
    }

    @Composable
    fun loginBackground(): Brush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF3EE),
                Color(0xFFFFE8DE),
                Color(0xFFFFF8F5),
            ),
        )
    }

    @Composable
    fun loginBackgroundDark(): Brush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A0F0A),
                Color(0xFF0F0D0C),
                Color(0xFF121110),
            ),
        )
    }

    fun cardAccent(start: Color, end: Color): Brush = Brush.horizontalGradient(
        colors = listOf(start, end),
    )
}
