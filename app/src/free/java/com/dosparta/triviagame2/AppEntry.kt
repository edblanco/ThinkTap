package com.dosparta.triviagame2

import androidx.compose.runtime.Composable
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel

@Composable
@Suppress("UNUSED_PARAMETER")
fun AppEntry(viewModel: TriviaViewModel, content: @Composable () -> Unit) {
    content()
}
