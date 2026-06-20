package com.example.ppb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.ppb.data.model.Screens
import com.example.ppb.ui.screens.ConfigurationScreen
import com.example.ppb.ui.screens.MenuScreen
import com.example.ppb.ui.theme.PPBTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PPBTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp()
                }
            }
        }
    }
}

@Composable
fun MyApp() {
    val backStack = remember { mutableStateListOf<Screens>(Screens.Menu) }
    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.count() > 1)
                backStack.removeLastOrNull()
        },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryProvider = entryProvider {
            entry<Screens.Menu> {
                MenuScreen(onNavConfig = { backStack.add(Screens.Config) })
            }
            entry<Screens.Config> {
                ConfigurationScreen(onNavBack = {
                    if (backStack.count() > 1)
                        backStack.removeLastOrNull()
                })
            }
        }
    )
}
