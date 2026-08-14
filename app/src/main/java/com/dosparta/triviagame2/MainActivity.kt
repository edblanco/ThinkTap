package com.dosparta.triviagame2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dosparta.trivia.ui.navigation.TriviaNavHost
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import com.dosparta.triviagame2.ui.theme.TriviaGame2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TriviaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.statusBars())

        setContent {
            TriviaGame2Theme {
                TriviaNavHost()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save game state when app goes to background
        viewModel.saveGameOnPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }
}