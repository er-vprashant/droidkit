package com.prashant.droidkit.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prashant.droidkit.DroidKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) DroidKit.refreshNotification()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        seedSampleData()
        requestNotificationPermission()
        makeTestNetworkCalls()

        setContent {
            SampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0D)
                ) {
                    SampleApp(
                        deepLinkUri = intent?.data?.toString(),
                        onOpenDroidKit = { DroidKit.launch(this) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun makeTestNetworkCalls() {
        val api = TestApi.create()
        lifecycleScope.launch {
            try {
                delay(500)
                api.getProducts(limit = 10)
                
                delay(300)
                api.getProduct(5)
                
                delay(400)
                api.searchProducts("phone")
                
                delay(300)
                api.getUsers()
                
                delay(400)
                api.getUser(3)
                
                delay(500)
                api.getCarts()
                
                delay(300)
                api.getCart(1)
                
                delay(400)
                api.addCart(
                    mapOf(
                        "userId" to 1,
                        "products" to listOf(
                            mapOf("id" to 1, "quantity" to 2),
                            mapOf("id" to 50, "quantity" to 1)
                        )
                    )
                )
                
                delay(400)
                api.updateCart(
                    1,
                    mapOf(
                        "merge" to false,
                        "products" to listOf(
                            mapOf("id" to 1, "quantity" to 5)
                        )
                    )
                )
                
                delay(300)
                api.getPosts(limit = 5)
                
                delay(400)
                api.getPost(1)
                
                delay(500)
                api.addPost(
                    mapOf(
                        "title" to "Test Post from DroidKit Demo",
                        "body" to "This is a test post created via the Network Inspector demo",
                        "userId" to 5
                    )
                )
                
                delay(300)
                api.getComments(limit = 5)
                
                delay(400)
                api.deleteCart(1)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun seedSampleData() {
        getSharedPreferences("user_settings", Context.MODE_PRIVATE).edit().apply {
            putBoolean("dark_mode", true)
            putBoolean("notifications_enabled", true)
            putString("username", "prashant")
            putString("locale", "en_IN")
            putInt("app_open_count", 42)
            putInt("theme_id", 2)
            putLong("last_sync_timestamp", System.currentTimeMillis())
            putFloat("volume_level", 0.75f)
            apply()
        }

        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("access_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
            putString("refresh_token", "dGhpcyBpcyBhIHNhbXBsZSByZWZy...")
            putLong("token_expiry", System.currentTimeMillis() + 3600000)
            putBoolean("is_logged_in", true)
            apply()
        }

        getSharedPreferences("feature_flags", Context.MODE_PRIVATE).edit().apply {
            putBoolean("new_onboarding", true)
            putBoolean("ab_test_checkout", false)
            putBoolean("enable_analytics", true)
            putString("experiment_group", "control_v2")
            apply()
        }
    }
}

@Composable
private fun SampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6C63FF),
            secondary = Color(0xFF1DB954),
            surface = Color(0xFF1A1A1A),
            background = Color(0xFF0D0D0D),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFF9E9E9E)
        ),
        content = content
    )
}

@Composable
private fun SampleApp(deepLinkUri: String?, onOpenDroidKit: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { -40 }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6C63FF), Color(0xFF1DB954))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "DroidKit",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Debug Toolkit for Android",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            "Zero-config. Debug-only. One line to integrate.",
            fontSize = 13.sp,
            color = Color(0xFF6C63FF),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Deep link banner
        if (deepLinkUri != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1DB954).copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Deep link received", fontSize = 12.sp, color = Color(0xFF1DB954), fontWeight = FontWeight.SemiBold)
                        Text(deepLinkUri, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Feature cards
        Text("What's inside", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            icon = Icons.Default.Storage,
            title = "Storage Inspector",
            description = "Browse SharedPreferences and SQLite databases. Edit values live, search keys, view table rows.",
            accent = Color(0xFF185FA5)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeatureCard(
            icon = Icons.Default.Link,
            title = "Deep Link Tester",
            description = "Fire any URI with custom extras. Save presets, replay from history, validate schemes.",
            accent = Color(0xFF3B6D11)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeatureCard(
            icon = Icons.Default.Notifications,
            title = "Push Notification Tester",
            description = "Compose and send local notifications with live preview. Test channels, deep link tap actions.",
            accent = Color(0xFFBA7517)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeatureCard(
            icon = Icons.Default.PhoneAndroid,
            title = "Network Inspector",
            description = "Inspect HTTP calls, mock responses, test error scenarios. Copy as cURL, filter by method/status.",
            accent = Color(0xFF9C27B0)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // How to launch section
        Text("How to launch", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LaunchMethodChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Vibration,
                label = "Shake device"
            )
            LaunchMethodChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Notifications,
                label = "Tap notification"
            )
            LaunchMethodChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PhoneAndroid,
                label = "Button below"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Open DroidKit button
        Button(
            onClick = onOpenDroidKit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            Icon(Icons.Default.Rocket, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open DroidKit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Integration snippet
        Text("Integration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "// build.gradle.kts\ndebugImplementation(\"com.prashant:droidkit:1.0.0\")\n\n// That's it. Zero config needed.",
                modifier = Modifier.padding(16.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFFB0B0B0)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Footer
        Text(
            "Built by Prashant Verma",
            fontSize = 12.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFF9E9E9E))
            }
        }
    }
}

@Composable
private fun LaunchMethodChip(modifier: Modifier = Modifier, icon: ImageVector, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFFB0B0B0), textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D, name = "Sample App")
@Composable
private fun SampleAppPreview() {
    SampleTheme {
        Surface(color = Color(0xFF0D0D0D)) {
            SampleApp(deepLinkUri = null, onOpenDroidKit = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D, name = "With Deep Link")
@Composable
private fun SampleAppWithDeepLinkPreview() {
    SampleTheme {
        Surface(color = Color(0xFF0D0D0D)) {
            SampleApp(deepLinkUri = "app://orders/latest", onOpenDroidKit = {})
        }
    }
}
