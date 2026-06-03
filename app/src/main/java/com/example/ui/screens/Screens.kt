package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.db.*
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.KeyRoomViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- MAIN WRAPPER WITH NAVIGATION BAR ---
@Composable
fun MainLayout(viewModel: KeyRoomViewModel) {
    val currentRoute by viewModel.currentRoute.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    var isUnlocked by remember { mutableStateOf(false) }

    // Display master screen locker if Master PIN is active
    if (securityPin.isNotEmpty() && !isUnlocked) {
        LockerScreen(pin = securityPin, onCorrectPin = { isUnlocked = true })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurfaceLevel1,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val navItems = listOf(
                        Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
                        Triple("explorer", "Explorer", Icons.Default.Apps),
                        Triple("database", "Database", Icons.Default.Storage),
                        Triple("preferences", "Prefs", Icons.Default.Tune),
                        Triple("backup", "Backups", Icons.Default.Backup)
                    )
                    navItems.forEach { (route, label, icon) ->
                        val selected = currentRoute == route || (route == "database" && currentRoute.startsWith("database"))
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(route) },
                            icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldAccent,
                                selectedTextColor = EmeraldAccent,
                                indicatorColor = DarkSurfaceLevel2,
                                unselectedIconColor = SlateGrey,
                                unselectedTextColor = SlateGrey
                            ),
                            modifier = Modifier.testTag("nav_item_$route")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkSlateBg, Color(0xFF07090C))
                        )
                    )
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "routes"
                ) { route ->
                    when {
                        route == "dashboard" -> DashboardScreen(viewModel)
                        route == "explorer" -> AppExplorerScreen(viewModel)
                        route == "database" -> DatabaseManagerScreen(viewModel)
                        route == "preferences" -> PreferencesManagerScreen(viewModel)
                        route == "backup" -> BackupCenterScreen(viewModel)
                        route == "analytics" -> StorageAnalyticsScreen(viewModel)
                        route == "files" -> DataManagementScreen(viewModel)
                        route == "settings" -> SettingsScreen(viewModel)
                        route == "apk_analyzer" -> ApkAnalyzerScreen(viewModel)
                        else -> DashboardScreen(viewModel)
                    }
                }
            }
        }
    }
}

// --- APP MASTER LOCKER SCREEN ---
@Composable
fun LockerScreen(pin: String, onCorrectPin: () -> Unit) {
    var enteredText by remember { mutableStateOf("") }
    var shakeTrigger by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock",
            tint = EmeraldAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "WORKSPACE ENCRYPTED",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Text(
            "Enter master security passcode to access KeyRoom modules.",
            fontSize = 12.sp,
            color = SlateGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Bullet representation of values
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            for (i in 1..4) {
                val isActive = enteredText.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isActive) EmeraldAccent else Color.White.copy(alpha = 0.2f))
                        .border(
                            width = 1.dp,
                            color = if (isActive) EmeraldAccent else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Grid PIN panel
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "OK")
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEach { char ->
                        Button(
                            onClick = {
                                when (char) {
                                    "Clear" -> {
                                        if (enteredText.isNotEmpty()) enteredText = enteredText.dropLast(1)
                                    }
                                    "OK" -> {
                                        if (enteredText == pin) {
                                            onCorrectPin()
                                        } else {
                                            enteredText = ""
                                            shakeTrigger = !shakeTrigger
                                        }
                                    }
                                    else -> {
                                        if (enteredText.length < 4) {
                                            enteredText += char
                                        }
                                        if (enteredText.length == 4 && enteredText == pin) {
                                            onCorrectPin()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (char == "OK") EmeraldAccent else DarkSurfaceLevel1,
                                contentColor = if (char == "OK") Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("pin_btn_$char")
                        ) {
                            Text(char, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- FEATURE SCREEN COMMON HEADER ---
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = SlateGrey,
                    fontWeight = FontWeight.Medium
                )
            }
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: KeyRoomViewModel) {
    val context = LocalContext.current
    val recentActivities by viewModel.recentActivities.collectAsState()
    val appsCount by viewModel.filteredApps.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val databases by viewModel.detectedDatabases.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStorageStats()
        viewModel.refreshDatabases()
        viewModel.refreshPreferenceFiles()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Enterprise Title Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "KEYROOM",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldAccent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Enterprise Diagnostics Platform",
                        fontSize = 12.sp,
                        color = SlateGrey
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo("settings") },
                    modifier = Modifier
                        .background(DarkSurfaceLevel1, RoundedCornerShape(12.dp))
                        .testTag("dashboard_settings_btn")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        // Summary Statistics Grid Slider
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardMetricCard(
                    title = "System Apps",
                    value = "${appsCount.size}",
                    icon = Icons.Default.Apps,
                    color = CobaltBlue,
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricCard(
                    title = "Databases",
                    value = "${databases.size}",
                    icon = Icons.Default.Storage,
                    color = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardMetricCard(
                    title = "Total Backups",
                    value = "${backups.size}",
                    icon = Icons.Default.Backup,
                    color = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                val totalSpace = storageStats["Total Free Space"] ?: 0L
                val spaceText = if (totalSpace > 0) Formatter.formatShortFileSize(context, totalSpace) else "N/A"
                DashboardMetricCard(
                    title = "Free Storage",
                    value = spaceText,
                    icon = Icons.Default.Memory,
                    color = CrimsonError,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Navigation Launchpad Grid
        item {
            Text("ADVANCED ACTIONS LAUNCHPAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGrey, letterSpacing = 1.sp)
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val launchpad = listOf(
                    Triple("File Manager", Icons.Default.Folder, "files"),
                    Triple("APK Anal", Icons.Default.Troubleshoot, "apk_analyzer"),
                    Triple("Analytics", Icons.Default.PieChart, "analytics")
                )
                items(launchpad) { (name, icon, route) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewModel.navigateTo(route) }
                            .testTag("launchpad_btn_$route"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(icon, contentDescription = name, tint = EmeraldAccent, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // Recent System Activity Trace logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("REAL-TIME DIAGNOSTIC SEQUENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGrey, letterSpacing = 1.sp)
                TextButton(
                    onClick = { viewModel.clearLogActivities() },
                    modifier = Modifier.testTag("clear_logs_btn")
                ) {
                    Text("Clear Stream", fontSize = 12.sp, color = CrimsonError)
                }
            }
        }

        if (recentActivities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1.copy(alpha = 0.5f))
                ) {
                    Text(
                        "Diagnostic buffer empty. All modules operational.",
                        fontSize = 12.sp,
                        color = SlateGrey,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(recentActivities.take(5)) { act ->
                ActivityLogItem(act)
            }
        }
    }
}

@Composable
fun DashboardMetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGrey, letterSpacing = 0.5.sp)
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
fun ActivityLogItem(act: RecentActivity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (act.status == "SUCCESS") EmeraldAccent else CrimsonError)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(act.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatter.format(Date(act.timestamp)), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SlateGrey)
                }
                if (act.description.isNotEmpty()) {
                    Text(act.description, fontSize = 11.sp, color = SlateGrey, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ==========================================
// 2. APPLICATION EXPLORER SCREEN
// ==========================================
@Composable
fun AppExplorerScreen(viewModel: KeyRoomViewModel) {
    val context = LocalContext.current
    val apps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.appsSearchQuery.collectAsState()
    val filterSystem by viewModel.filterSystem.collectAsState()
    var selectedAppDetails by remember { mutableStateOf<AppInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "APPLICATION EXPLORER",
            subtitle = "Security scan and package signature decoder"
        )

        // Search and Filter controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateAppsSearch(it) },
                placeholder = { Text("Search package or label...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SlateGrey) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceLevel1,
                    unfocusedContainerColor = DarkSurfaceLevel1,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("app_search_field")
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { viewModel.toggleSystemAppsFilter() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (filterSystem) DarkSurfaceLevel1 else EmeraldAccent,
                    contentColor = if (filterSystem) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("app_filter_btn")
            ) {
                Text(if (filterSystem) "User Only" else "System Inc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Apps List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAppDetails = app }
                        .testTag("app_item_${app.packageName}"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = DarkSurfaceLevel2
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (app.isSystemApp) Icons.Default.SystemUpdate else Icons.Default.Android,
                                    contentDescription = null,
                                    tint = if (app.isSystemApp) CobaltBlue else EmeraldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text(app.packageName, fontSize = 11.sp, color = SlateGrey, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Formatter.formatShortFileSize(context, app.sizeBytes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("v${app.versionName}", fontSize = 11.sp, color = SlateGrey)
                        }
                    }
                }
            }
        }
    }

    // Details Modal
    if (selectedAppDetails != null) {
        val app = selectedAppDetails!!
        AlertDialog(
            onDismissRequest = { selectedAppDetails = null },
            title = { Text(app.appName, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Package: ${app.packageName}", fontSize = 12.sp, color = SlateGrey)
                    Text("Version: ${app.versionName} (Build ${app.versionCode})", fontSize = 12.sp, color = Color.White)
                    Text("Target SDK: ${app.targetSdkVersion}", fontSize = 12.sp, color = Color.White)
                    Text("System Application: ${if (app.isSystemApp) "YES" else "NO"}", fontSize = 12.sp, color = IfColorSystem(app.isSystemApp))
                    Text("Install Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(app.firstInstallTime))}", fontSize = 11.sp, color = SlateGrey)
                    Text("Last Updated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(app.lastUpdateTime))}", fontSize = 11.sp, color = SlateGrey)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppDetails = null }) {
                    Text("Close Diagnostics", color = EmeraldAccent)
                }
            },
            containerColor = DarkSurfaceLevel1,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

fun IfColorSystem(isSys: Boolean): Color {
    return if (isSys) CobaltBlue else EmeraldAccent
}

// ==========================================
// 3. APK ANALYZER SCREEN
// ==========================================
@Composable
fun ApkAnalyzerScreen(viewModel: KeyRoomViewModel) {
    val context = LocalContext.current
    val mockApkPath = remember { File(context.filesDir, "sandbox_workspace/keyroom_installer_pack.apk").absolutePath }
    val apkAnalysis by viewModel.apkAnalysis.collectAsState()
    val errTrace by viewModel.apkAnalysisError.collectAsState()

    // Ensure dummy simulated APK exists inside workspace for instant testing
    LaunchedEffect(Unit) {
        val workspace = File(context.filesDir, "sandbox_workspace")
        if (!workspace.exists()) workspace.mkdirs()
        val mockFile = File(mockApkPath)
        if (!mockFile.exists()) {
            mockFile.writeText("SIMULATED BINARY APK CONTAINER STREAM HEADER")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "APK BINARY ANALYZER",
            subtitle = "Extract, decompile, and inspect permission parameters"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Local File", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = "", tint = EmeraldAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("keyroom_installer_pack.apk", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Color.White)
                    Button(
                        onClick = { viewModel.analyzeLocalApkFile(mockApkPath) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                        modifier = Modifier.testTag("analyze_apk_btn")
                    ) {
                        Text("Inspect APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        errTrace?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CrimsonError.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, CrimsonError)
            ) {
                Text("Failed file format validation: $err", color = CrimsonError, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
        }

        apkAnalysis?.let { pack ->
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("DECODED BINARY MANIFEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGrey, letterSpacing = 1.sp)
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Label Target", fontSize = 12.sp, color = SlateGrey)
                                Text(pack.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Package String", fontSize = 12.sp, color = SlateGrey)
                                Text(pack.packageName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Build Revision", fontSize = 12.sp, color = SlateGrey)
                                Text("v${pack.versionName} (${pack.versionCode})", fontSize = 13.sp, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Target Android SDK", fontSize = 12.sp, color = SlateGrey)
                                Text("API ${pack.targetSdkVersion}", fontSize = 13.sp, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Minimum Requirements", fontSize = 12.sp, color = SlateGrey)
                                Text("API ${pack.minSdkVersion}", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }

                item {
                    Text("DECLARED PERMISSIONS (${pack.permissions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGrey, letterSpacing = 1.sp)
                }

                if (pack.permissions.isEmpty()) {
                    item {
                        Text("No external security permissions declared in manifest.", fontSize = 12.sp, color = SlateGrey)
                    }
                } else {
                    items(pack.permissions) { perm ->
                        val isRisky = perm.contains("WRITE") || perm.contains("SYSTEM")
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isRisky) Color(0xFF2D1F1F) else DarkSurfaceLevel1),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isRisky) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = "",
                                    tint = if (isRisky) CrimsonError else EmeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(perm.substringAfterLast("."), fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CodeOff, contentDescription = "", tint = SlateGrey, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Run visual inspect above to decode static binary stream.", fontSize = 12.sp, color = SlateGrey)
                }
            }
        }
    }
}

// ==========================================
// 4. DATA MANAGEMENT (FILE EXPLORER) SCREEN
// ==========================================
@Composable
fun DataManagementScreen(viewModel: KeyRoomViewModel) {
    val context = LocalContext.current
    val currentPath by viewModel.currentDirectoryPath.collectAsState()
    val filesList by viewModel.currentDirFiles.collectAsState()

    var showFolderDialog by remember { mutableStateOf(false) }
    var folderInputName by remember { mutableStateOf("") }

    var showFileDialog by remember { mutableStateOf(false) }
    var fileInputName by remember { mutableStateOf("") }
    var fileInputContent by remember { mutableStateOf("") }

    LaunchedEffect(currentPath) {
        viewModel.loadWorkspaceDirectory(currentPath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "WORKSPACE DATA CENTER",
            subtitle = "Active context: ${if (currentPath.isEmpty()) "Files Sandbox Root" else currentPath.substringAfterLast("/")}",
            onBack = if (currentPath.isNotEmpty()) {
                {
                    val parts = currentPath.split("/")
                    val parent = parts.dropLast(1).joinToString("/")
                    viewModel.loadWorkspaceDirectory(parent)
                }
            } else null,
            actions = {
                IconButton(onClick = { showFolderDialog = true }, modifier = Modifier.testTag("create_folder_btn")) {
                    Icon(Icons.Default.CreateNewFolder, "New Folder", tint = EmeraldAccent)
                }
                IconButton(onClick = { showFileDialog = true }, modifier = Modifier.testTag("create_file_btn")) {
                    Icon(Icons.Default.NoteAdd, "New File", tint = EmeraldAccent)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filesList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "", tint = SlateGrey, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Folder empty. Use active triggers to compose nodes.", fontSize = 12.sp, color = SlateGrey)
                        }
                    }
                }
            } else {
                items(filesList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.isDirectory) {
                                    viewModel.loadWorkspaceDirectory(item.path)
                                }
                            }
                            .testTag("file_item_${item.name}"),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = "",
                                tint = if (item.isDirectory) CobaltBlue else EmeraldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "${Formatter.formatShortFileSize(context, item.sizeBytes)} • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(item.lastModified))}",
                                    fontSize = 11.sp, color = SlateGrey
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteWorkspaceItem(item.path) },
                                modifier = Modifier.testTag("delete_file_${item.name}")
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Compose Workspace Folder", color = Color.White) },
            text = {
                TextField(
                    value = folderInputName,
                    onValueChange = { folderInputName = it },
                    placeholder = { Text("Directory label Name") },
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderInputName.isNotEmpty()) {
                            viewModel.createWorkspaceFolder(folderInputName)
                            folderInputName = ""
                            showFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black)
                ) {
                    Text("Provision")
                }
            },
            containerColor = DarkSurfaceLevel1
        )
    }

    // New File Dialog
    if (showFileDialog) {
        AlertDialog(
            onDismissRequest = { showFileDialog = false },
            title = { Text("Compose Document", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = fileInputName,
                        onValueChange = { fileInputName = it },
                        placeholder = { Text("Filename (e.g. dump.txt)") },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    TextField(
                        value = fileInputContent,
                        onValueChange = { fileInputContent = it },
                        placeholder = { Text("Core payload contents") },
                        modifier = Modifier.height(100.dp),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileInputName.isNotEmpty()) {
                            viewModel.createWorkspaceFile(fileInputName, fileInputContent)
                            fileInputName = ""
                            fileInputContent = ""
                            showFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black)
                ) {
                    Text("Commit Write")
                }
            },
            containerColor = DarkSurfaceLevel1
        )
    }
}

// ==========================================
// 5. PREFERENCES MANAGER SCREEN
// ==========================================
@Composable
fun PreferencesManagerScreen(viewModel: KeyRoomViewModel) {
    val context = LocalContext.current
    val prefFiles by viewModel.preferenceFiles.collectAsState()
    val selectedFile by viewModel.selectedPrefFile.collectAsState()
    val entries by viewModel.filteredPrefEntries.collectAsState()
    val querySearch by viewModel.prefSearch.collectAsState()

    var showEditDialog by remember { mutableStateOf<PreferenceEntry?>(null) }
    var editValueText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshPreferenceFiles()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "XML CONFIG PREFS MANAGER",
            subtitle = if (selectedFile != null) "Inspecting settings register: ${selectedFile}.xml" else "Scan, inject, and overwrite SharedPreferences XML",
            onBack = if (selectedFile != null) {
                { viewModel.selectPreferenceFile(null) }
            } else null
        )

        if (selectedFile == null) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(prefFiles) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPreferenceFile(file.name) }
                            .testTag("pref_file_item_${file.name}"),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "", tint = EmeraldAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${file.name}.xml", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Keys Registered: ${file.keysCount}", fontSize = 11.sp, color = SlateGrey)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = "", tint = SlateGrey)
                        }
                    }
                }
            }
        } else {
            // Searcb bar
            TextField(
                value = querySearch,
                onValueChange = { viewModel.updatePrefSearch(it) },
                placeholder = { Text("Filter setting constants...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceLevel1,
                    unfocusedContainerColor = DarkSurfaceLevel1,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("pref_search_field")
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.key, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(entry.type.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent, modifier = Modifier.background(DarkSurfaceLevel2, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(entry.value, fontSize = 12.sp, color = SlateGrey, fontFamily = FontFamily.Monospace)
                            }
                            IconButton(
                                onClick = {
                                    editValueText = entry.value
                                    showEditDialog = entry
                                },
                                modifier = Modifier.testTag("edit_pref_${entry.key}")
                            ) {
                                Icon(Icons.Default.Edit, "Edit", tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { viewModel.deletePrefKey(selectedFile!!, entry.key) },
                                modifier = Modifier.testTag("delete_pref_${entry.key}")
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = CrimsonError, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit value dialog
    if (showEditDialog != null) {
        val entry = showEditDialog!!
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit dynamic parameter configuration", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KEY: ${entry.key}", fontSize = 12.sp, color = SlateGrey)
                    TextField(
                        value = editValueText,
                        onValueChange = { editValueText = it },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePrefValue(selectedFile!!, entry.key, editValueText, entry.type)
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black)
                ) {
                    Text("Apply Inject")
                }
            },
            containerColor = DarkSurfaceLevel1
        )
    }
}

// ==========================================
// 6. DATABASE MANAGER SCREEN
// ==========================================
@Composable
fun DatabaseManagerScreen(viewModel: KeyRoomViewModel) {
    val dbs by viewModel.detectedDatabases.collectAsState()
    val selectedDb by viewModel.selectedDatabase.collectAsState()
    val tableSelected by viewModel.selectedTable.collectAsState()
    val sqlOutput by viewModel.sqlOutput.collectAsState()
    val sqlInputText by viewModel.sqlQueryText.collectAsState()
    var savedQueryTitle by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshDatabases()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "SQLITE METADATA ENGINE",
            subtitle = if (selectedDb != null) "Active SQLite DB: ${selectedDb!!.name}" else "Scan real tables, inspect schemas, run transactions",
            onBack = if (selectedDb != null) {
                { viewModel.selectDatabase(null) }
            } else null
        )

        if (selectedDb == null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dbs) { db ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectDatabase(db) }
                            .testTag("db_item_${db.name}"),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = "", tint = EmeraldAccent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(db.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Detected Local Master Tables: ${db.tables.size}", fontSize = 11.sp, color = SlateGrey)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Show interactive terminal editor with raw query execute functionality
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Horizontal scroll tables buttons
                Text("TABLES INDEX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGrey)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedDb!!.tables.forEach { table ->
                        val isSelfSelected = tableSelected == table
                        Button(
                            onClick = { viewModel.selectDatabaseTable(table) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelfSelected) EmeraldAccent else DarkSurfaceLevel1,
                                contentColor = if (isSelfSelected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("table_select_$table")
                        ) {
                            Text(table, fontSize = 11.sp)
                        }
                    }
                }

                // Query code block terminal
                Text("INTEGRATED TRANSACTION TERMINAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGrey)
                TextField(
                    value = sqlInputText,
                    onValueChange = { viewModel.updateSqlQuery(it) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Green),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceLevel2,
                        unfocusedContainerColor = DarkSurfaceLevel2
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(6.dp))
                        .testTag("sql_terminal_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.executeCurrentSql() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_query_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Execute Instruction", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceLevel1),
                        modifier = Modifier.testTag("save_query_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "", tint = Color.White)
                    }
                }

                // Grid Output
                sqlOutput?.let { tableRow ->
                    Text("SQL DISPATCH RESULT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGrey)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .background(DarkSurfaceLevel2)
                                    .padding(8.dp)
                            ) {
                                tableRow.columns.forEach { col ->
                                    Text(
                                        col.uppercase(),
                                        modifier = Modifier.width(110.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                }
                            }

                            // Dynamic Rows
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                val colsCount = tableRow.columns.size
                                val cells = tableRow.cells
                                if (cells.isEmpty()) {
                                    Text("0 rows fetched.", fontSize = 12.sp, color = SlateGrey, modifier = Modifier.padding(16.dp))
                                } else {
                                    for (rowIndex in 0 until (cells.size / colsCount)) {
                                        Row(
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            for (colIndex in 0 until colsCount) {
                                                val cellValue = cells[rowIndex * colsCount + colIndex]
                                                Text(
                                                    cellValue,
                                                    modifier = Modifier.width(110.dp),
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save SQL Command Bookmark", color = Color.White) },
            text = {
                TextField(
                    value = savedQueryTitle,
                    onValueChange = { savedQueryTitle = it },
                    placeholder = { Text("Bookmark reference title (e.g. Activity Query)") },
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (savedQueryTitle.isNotEmpty()) {
                            viewModel.saveCurrentSql(savedQueryTitle)
                            savedQueryTitle = ""
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black)
                ) {
                    Text("Bookmark")
                }
            },
            containerColor = DarkSurfaceLevel1
        )
    }
}

// ==========================================
// 7. BACKUP CENTER SCREEN
// ==========================================
@Composable
fun BackupCenterScreen(viewModel: KeyRoomViewModel) {
    val context = LocalContext.current
    val backupList by viewModel.backups.collectAsState()

    var checkedDbs by remember { mutableStateOf(true) }
    var checkedXmls by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "SECURE BACKUP VAULT",
            subtitle = "Perform zip dumps with physical isolation safeguards"
        )

        // Selective Dump selection card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Selective Module Dump Configuration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checkedDbs,
                        onCheckedChange = { checkedDbs = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldAccent)
                    )
                    Text("Active SQLite Databases (.db)", fontSize = 12.sp, color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checkedXmls,
                        onCheckedChange = { checkedXmls = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldAccent)
                    )
                    Text("System Preferences XML files (.xml)", fontSize = 12.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val types = mutableListOf<String>()
                            if (checkedDbs) types.add("DATABASE")
                            if (checkedXmls) types.add("PREFERENCES")
                            if (types.isNotEmpty()) {
                                viewModel.backupSelected(types)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("selective_backup_btn")
                    ) {
                        Text("Selective Pack", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = { viewModel.backupFullSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceLevel1),
                        border = BorderStroke(1.dp, EmeraldAccent),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("full_backup_btn")
                    ) {
                        Text("Full Pack", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List history backups
        Text("VAULT RECORD ENTRIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGrey, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (backupList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "No dump archives detected inside vault registers.",
                            fontSize = 12.sp,
                            color = SlateGrey,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(backupList) { pack ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "", tint = AmberWarning, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pack.name, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row {
                                    Text(pack.type, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.background(EmeraldAccent, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${Formatter.formatShortFileSize(context, pack.size)} • ${pack.itemCount} items", fontSize = 11.sp, color = SlateGrey)
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteBackup(pack) },
                                modifier = Modifier.testTag("delete_backup_${pack.id}")
                            ) {
                                Icon(Icons.Default.Delete, "Delete File", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. STORAGE ANALYTICS SCREEN
// ==========================================
@Composable
fun StorageAnalyticsScreen(viewModel: KeyRoomViewModel) {
    val context = LocalContext.current
    val statsReport by viewModel.storageStats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStorageStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "SPACE DENSITY ANALYTICS",
            subtitle = "Calculated storage footprint ratios across registers"
        )

        // Custom Vector Pie/Arc Drawing Block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Space Allocation Metric Arcs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 16.dp.toPx()
                        // Draw Background Track
                        drawArc(
                            color = Color.White.copy(alpha = 0.05f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeW)
                        )
                        // Draw Allocation Ratios
                        // DB = 30%, Prefs = 15%, Cache = 25%, Files = 20%
                        drawArc(
                            color = EmeraldAccent,
                            startAngle = -90f,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = CobaltBlue,
                            startAngle = 30f,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = AmberWarning,
                            startAngle = 120f,
                            sweepAngle = 70f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Operational", fontSize = 11.sp, color = SlateGrey)
                        Text("100% Ok", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Legand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem("Databases", EmeraldAccent)
                    LegendItem("Prefs XML", CobaltBlue)
                    LegendItem("Sandbox Docs", AmberWarning)
                }
            }
        }

        // Storage size list reports
        Text("DENSITY BREAKDOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateGrey, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statsReport.filter { it.key != "Total Free Space" }.forEach { (name, size) ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(Formatter.formatShortFileSize(context, size), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = EmeraldAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, color = SlateGrey)
    }
}

// ==========================================
// 9. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: KeyRoomViewModel) {
    val activeTheme by viewModel.themeAccent.collectAsState()
    val activePin by viewModel.securityPin.collectAsState()
    val biometricsActive by viewModel.biometricAuth.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var inputPinString by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "KEYROOM SYSTEM CORE SETTINGS",
            subtitle = "Workspace settings, themes, and master lock security layers"
        )

        // Theme picker card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dynamic Theme Aesthetic", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val themes = listOf("Dark Cosmic Slate", "Emerald Matrix", "Light Professional")
                    themes.forEach { theme ->
                        val isSel = activeTheme == theme
                        Button(
                            onClick = { viewModel.changeThemeAccent(theme) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) EmeraldAccent else DarkSurfaceLevel2,
                                contentColor = if (isSel) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("theme_btn_$theme")
                        ) {
                            Text(theme.substringBefore(" "), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Security parameters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enterprise Security Locks", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Master App Locker PIN", fontSize = 12.sp, color = Color.White)
                        Text(if (activePin.isEmpty()) "Passcode lock deactivated" else "Encrypted PIN Active", fontSize = 10.sp, color = SlateGrey)
                    }
                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                        modifier = Modifier.testTag("set_pin_btn")
                    ) {
                        Text(if (activePin.isEmpty()) "Define PIN" else "Overriding PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Workspace Biometric Access", fontSize = 12.sp, color = Color.White)
                        Text("Authorize scans using face/fingerprint parameters", fontSize = 10.sp, color = SlateGrey)
                    }
                    Switch(
                        checked = biometricsActive,
                        onCheckedChange = { viewModel.toggleBiometrics() },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent),
                        modifier = Modifier.testTag("biometrics_switch")
                    )
                }
            }
        }

        // System Diagnostic report
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Diagnostics Report", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("KeyRoom Core System is operational.", fontSize = 12.sp, color = EmeraldAccent)
                Text("Runtime Platform: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", fontSize = 11.sp, color = SlateGrey)
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configure Security Lock Code", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide 4 digits (e.g. 1234)", fontSize = 12.sp, color = SlateGrey)
                    TextField(
                        value = inputPinString,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputPinString = it },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPinString.length == 4) {
                            viewModel.setSecurityPin(inputPinString)
                            inputPinString = ""
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black)
                ) {
                    Text("Lock Register")
                }
            },
            containerColor = DarkSurfaceLevel1
        )
    }
}
