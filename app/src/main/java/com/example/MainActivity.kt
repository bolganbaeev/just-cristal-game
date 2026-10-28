package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GameArenaScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HostLobbyScreen
import com.example.ui.screens.JoinLobbyScreen
import com.example.ui.screens.ScoresScreen
import com.example.ui.theme.CristalZombieTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CristalZombieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CristalZombieApp()
                }
            }
        }
    }
}

@Composable
fun CristalZombieApp(
    viewModel: GameViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.HOME -> HomeScreen(viewModel = viewModel)
            AppScreen.HOST_LOBBY -> HostLobbyScreen(viewModel = viewModel)
            AppScreen.JOIN_LOBBY -> JoinLobbyScreen(viewModel = viewModel)
            AppScreen.ARENA -> GameArenaScreen(viewModel = viewModel)
            AppScreen.SCORES -> ScoresScreen(viewModel = viewModel)
        }
    }
}
