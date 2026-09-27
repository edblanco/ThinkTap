package com.dosparta.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A soft vertical wash from the theme's primary container into the background.
 *
 * It gives every screen a shared sense of depth without introducing images, and it is drawn
 * beneath content so text contrast is unaffected.
 */
@Composable
fun BrandBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val brush = Brush.verticalGradient(
        0f to scheme.primaryContainer.copy(alpha = 0.45f),
        0.35f to scheme.background,
        1f to scheme.background
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .background(brush),
        content = content
    )
}

/** The app's top app bar: transparent so [BrandBackground] shows through. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriviaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

/**
 * A screen shell combining the brand background, a transparent app bar and insets handling.
 *
 * @param title the app bar title
 * @param modifier applied to the [Scaffold]
 * @param contentPadding extra padding applied inside the window insets
 * @param content the screen body, already inset-aware
 */
@Composable
fun TriviaScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    BrandBackground {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = { TriviaTopBar(title = title) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

/** An elevated, generously rounded card used to group related content. */
@Composable
fun TriviaCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        content()
    }
}

/** A small, rounded label used for metadata such as a question's category or difficulty. */
@Composable
fun TriviaMetaChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
