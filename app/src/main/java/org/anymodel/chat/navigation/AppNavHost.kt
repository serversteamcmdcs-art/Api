package org.anymodel.chat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.anymodel.chat.chat.ui.ChatScreen
import org.anymodel.chat.settings.ui.SettingsScreen

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.CHAT) {
        composable(Routes.CHAT) {
            ChatScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
