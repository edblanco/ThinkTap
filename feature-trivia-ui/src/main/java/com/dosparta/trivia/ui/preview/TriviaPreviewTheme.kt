package com.dosparta.trivia.ui.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.dosparta.core.ui.theme.TriviaGame2Theme

@Composable
internal fun TriviaPreviewTheme(content: @Composable () -> Unit) {
    TriviaGame2Theme {
        Surface(content = content)
    }
}
