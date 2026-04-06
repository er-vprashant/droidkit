package com.prashant.droidkit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.prashant.droidkit.ui.dashboard.DashboardScreen
import com.prashant.droidkit.ui.deeplink.DeepLinkTesterScreen
import com.prashant.droidkit.ui.network.MockEditorScreen
import com.prashant.droidkit.ui.network.MockListScreen
import com.prashant.droidkit.ui.network.NetworkDetailScreen
import com.prashant.droidkit.ui.network.NetworkInspectorScreen
import com.prashant.droidkit.ui.network.NetworkSetupScreen
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
        composable("network_setup") { NetworkSetupScreen(navController) }
        composable("network") { NetworkInspectorScreen(navController) }
        composable(
            "network/detail/{callId}",
            arguments = listOf(navArgument("callId") { type = NavType.StringType })
        ) { backStackEntry ->
            NetworkDetailScreen(
                navController = navController,
                callId = backStackEntry.arguments?.getString("callId") ?: ""
            )
        }
        composable("network/mocks") { MockListScreen(navController) }
        composable("network/mocks/new") { 
            MockEditorScreen(
                navController = navController,
                mockId = null,
                fromCallId = null
            )
        }
        composable(
            "network/mocks/new?from={callId}",
            arguments = listOf(navArgument("callId") { 
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            MockEditorScreen(
                navController = navController,
                mockId = null,
                fromCallId = backStackEntry.arguments?.getString("callId")
            )
        }
        composable(
            "network/mocks/edit/{mockId}",
            arguments = listOf(navArgument("mockId") { type = NavType.StringType })
        ) { backStackEntry ->
            MockEditorScreen(
                navController = navController,
                mockId = backStackEntry.arguments?.getString("mockId"),
                fromCallId = null
            )
        }
    }
}
