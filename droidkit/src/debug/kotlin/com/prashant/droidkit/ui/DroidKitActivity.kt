package com.prashant.droidkit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prashant.droidkit.ui.dashboard.DashboardScreen
import com.prashant.droidkit.ui.deeplink.DeepLinkTesterScreen
import com.prashant.droidkit.ui.notification.NotificationTesterScreen
import com.prashant.droidkit.ui.storage.StorageInspectorScreen
import com.prashant.droidkit.ui.theme.DroidKitTheme

class DroidKitActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroidKitTheme {
                DroidKitNavHost()
            }
        }
    }
}

@Composable
private fun DroidKitNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController) }
        composable("storage") { StorageInspectorScreen() }
        composable("deeplink") { DeepLinkTesterScreen() }
        composable("notifications") { NotificationTesterScreen() }
    }
}
