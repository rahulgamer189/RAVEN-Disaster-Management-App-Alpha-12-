package com.raven.application

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.raven.application.bluetooth.BluetoothDeviceDomain
import com.raven.application.bluetooth.BluetoothMessage
import com.raven.application.bluetooth.BluetoothViewModel
import com.raven.application.bluetooth.Camp
import com.raven.application.ui.PermissionWrapper
import com.raven.application.ui.theme.CyberGreen
import com.raven.application.ui.theme.RavenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = Configuration.getInstance()
        configuration.userAgentValue = "Raven/1.1 (com.raven.application; contact: supratik.nanda@gmail.com)"
        configuration.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        enableEdgeToEdge()
        setContent {
            RavenTheme {
                PermissionWrapper {
                    val viewModel: BluetoothViewModel = viewModel()
                    RavenApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun RavenApp(viewModel: BluetoothViewModel) {
    val navigationState = rememberNavigationState(
        startRoute = Discovery,
        topLevelRoutes = setOf(Discovery, MapRoute, CampRoute, Location, Settings)
    )
    val navigator = remember { Navigator(navigationState) }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<Discovery> {
            AdaptiveDiscoveryChat(
                viewModel = viewModel,
                navigator = navigator,
                onNavigateToSettings = { navigator.navigate(Settings) }
            )
        }
        entry<MapRoute> {
            MapScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() }
            )
        }
        entry<CampRoute> {
            CampScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() }
            )
        }
        entry<Location> {
            LocationScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() }
            )
        }
        entry<Settings> {
            SettingsScreen(onBack = { navigator.goBack() })
        }
    }

    Scaffold(
        bottomBar = {
            if (navigationState.topLevelRoute != Settings) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .height(64.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val routes = listOf(
                            Triple(Discovery, Icons.Default.Radar, stringResource(R.string.nav_nodes)),
                            Triple(MapRoute, Icons.Default.Explore, stringResource(R.string.nav_map)),
                            Triple(CampRoute, Icons.Default.Security, stringResource(R.string.nav_base)),
                            Triple(Location, Icons.Default.MyLocation, stringResource(R.string.nav_peers))
                        )
                        
                        routes.forEach { (route, icon, label) ->
                            val selected = navigationState.topLevelRoute == route
                            IconButton(onClick = { navigator.navigate(route) }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        icon, 
                                        contentDescription = label,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveDiscoveryChat(
    viewModel: BluetoothViewModel,
    navigator: Navigator,
    onNavigateToSettings: () -> Unit
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavKey>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isConnected, selectedDevice) {
        if (isConnected || selectedDevice != null) {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
        } else {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.List)
        }
    }

    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                DiscoveryScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (isConnected || selectedDevice != null) {
                    ChatScreen(
                        viewModel = viewModel,
                        onDisconnect = { viewModel.disconnect() },
                        canGoBack = scaffoldNavigator.canNavigateBack(),
                        onBack = {
                            coroutineScope.launch {
                                scaffoldNavigator.navigateBack()
                            }
                        },
                        onNavigateToLocation = { navigator.navigate(Location) }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.msg_select_device),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: BluetoothViewModel,
    onNavigateToSettings: () -> Unit
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.app_name_caps), 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 4.sp
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BentoSOSCard(
                    onSOS = { viewModel.sendMessage("SOS EMERGENCY BROADCAST!", type = "SOS") }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShareLocation,
                        title = stringResource(R.string.action_share_location),
                        subtitle = stringResource(R.string.subtitle_broadcast_gps),
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.shareLocation() }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = if (isScanning) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.Search,
                        title = if (isScanning) stringResource(R.string.action_scanning) else stringResource(R.string.action_scan_network),
                        subtitle = stringResource(R.string.subtitle_find_peers),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.startScanning() },
                        isLoading = isScanning
                    )
                }
            }

            if (pairedDevices.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.section_trusted_nodes), pairedDevices.size.toString())
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(pairedDevices) { device ->
                            ModernDeviceCard(device) { viewModel.connectToDevice(device) }
                        }
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.section_nearby_signals), scannedDevices.size.toString())
            }
            
            if (scannedDevices.isEmpty() && !isScanning) {
                item {
                    EmptyDiscoveryState()
                }
            } else {
                items(scannedDevices) { device ->
                    DiscoveryDeviceItem(device) { viewModel.connectToDevice(device) }
                }
            }
        }
    }
}

@Composable
fun BentoSOSCard(onSOS: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        onClick = onSOS
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Red.copy(alpha = 0.1f),
                    radius = size.maxDimension / 1.5f,
                    center = Offset(size.width, 0f)
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.BottomStart)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.action_emergency_sos),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    stringResource(R.string.subtitle_sos_broadcast),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = color)
                } else {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ModernDeviceCard(device: BluetoothDeviceDomain, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(device.name ?: stringResource(R.string.label_unnamed_node), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(device.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun DiscoveryDeviceItem(device: BluetoothDeviceDomain, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name ?: stringResource(R.string.label_unknown_device), style = MaterialTheme.typography.titleMedium)
                Text(device.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.Add, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Text(
                count, 
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun EmptyDiscoveryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Radar, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.msg_no_active_signals), 
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            stringResource(R.string.msg_initiate_scan), 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: BluetoothViewModel,
    onDisconnect: () -> Unit,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val peerTelemetry by viewModel.peerTelemetry.collectAsState()
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var showAssistant by remember { mutableStateOf(false) }
    var assistantAnswer by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.header_mesh_comm), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                        if (connectedDevices.isNotEmpty()) {
                            Text(stringResource(R.string.label_active_nodes, connectedDevices.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = stringResource(R.string.cd_disconnect), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        if (showAssistant) {
            ModalBottomSheet(
                onDismissRequest = { showAssistant = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.header_tactical_assistant), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(assistantAnswer, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAssistant = false }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_dismiss))
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (peerTelemetry.isNotEmpty()) {
                    NetworkStatusDashboard(peerTelemetry, onExpand = onNavigateToLocation)
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        val isFromMe = message.senderName == context.getString(R.string.label_sender_me)
                        ChatBubble(message = message, isFromMe = isFromMe)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasMedicalKeywords = remember(messageText) {
                        listOf("bleed", "fracture", "shock", "water", "burn", "cpr", "breathing", "injury").any { messageText.lowercase().contains(it) }
                    }
                    
                    IconButton(onClick = {
                        assistantAnswer = OfflineAssistant.answer(context, messageText)
                        showAssistant = true
                    }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.cd_ai_assistant),
                            tint = if (hasMedicalKeywords) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }

                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.hint_secure_message), style = MaterialTheme.typography.bodyMedium) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        maxLines = 4
                    )

                    IconButton(onClick = { viewModel.sendMessage("SOS EMERGENCY!", type = "SOS") }) {
                        Icon(Icons.Default.Report, contentDescription = stringResource(R.string.cd_sos), tint = MaterialTheme.colorScheme.error)
                    }

                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, 
                                contentDescription = stringResource(R.string.cd_send), 
                                modifier = Modifier.size(18.dp),
                                tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: BluetoothViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var osmMap: MapView? by remember { mutableStateOf(null) }
    var currentRouteOverlay: Polyline? by remember { mutableStateOf(null) }

    val camps by viewModel.camps.collectAsState()
    val peers by viewModel.peerTelemetry.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var isFollowMode by remember { mutableStateOf(false) }
    var locationOverlayState: MyLocationNewOverlay? by remember { mutableStateOf(null) }
    
    val sosPeers = peers.values.filter { it.messageType == "SOS" && it.latitude != null }

    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.header_tactical_map), style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        osmMap?.let { map ->
                            Toast.makeText(context, context.getString(R.string.msg_caching_area), Toast.LENGTH_SHORT).show()
                            val mgr = CacheManager(map)
                            mgr.downloadAreaAsync(context, map.boundingBox, 10, 16, object : CacheManager.CacheManagerCallback {
                                override fun onTaskComplete() { Toast.makeText(context, context.getString(R.string.msg_offline_ready), Toast.LENGTH_LONG).show() }
                                override fun onTaskFailed(errors: Int) { Toast.makeText(context, context.getString(R.string.msg_download_failed), Toast.LENGTH_SHORT).show() }
                                override fun updateProgress(p0: Int, p1: Int, p2: Int, p3: Int) {}
                                override fun setPossibleTilesInArea(p0: Int) {}
                                override fun downloadStarted() {}
                            })
                        }
                    }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.cd_download))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        val hotTiles: ITileSource = XYTileSource(
                            "OSMHOT", 0, 19, 256, ".png", 
                            arrayOf("https://a.tile.openstreetmap.fr/hot/", 
                                    "https://b.tile.openstreetmap.fr/hot/", 
                                    "https://c.tile.openstreetmap.fr/hot/")
                        )
                        setTileSource(hotTiles)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                            enableMyLocation()
                        }
                        locationOverlayState = locationOverlay
                        overlays.add(locationOverlay)
                        osmMap = this
                        controller.setCenter(GeoPoint(22.5, 79.0))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> 
                    view.onResume()
                    locationOverlayState?.let { overlay ->
                        if (isFollowMode) overlay.enableFollowLocation() 
                        else overlay.disableFollowLocation()
                    }
                    val currentOverlays = view.overlays
                    currentOverlays.removeAll { it is Marker }
                    
                    camps.forEach { camp ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(camp.latitude, camp.longitude)
                            title = camp.name
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        currentOverlays.add(marker)
                    }
                    
                    peers.values.filter { it.latitude != null }.forEach { peer ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(peer.latitude!!, peer.longitude!!)
                            title = peer.senderName
                            snippet = context.getString(R.string.label_battery_format, peer.batteryPercentage ?: 0)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        currentOverlays.add(marker)
                    }
                    view.invalidate()
                }
            )

            if (isScanning) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.label_scanning_mesh), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingTacticalAction(
                    icon = Icons.Default.MyLocation,
                    label = "CENTER_ME",
                    onClick = {
                        locationOverlayState?.myLocation?.let {
                            osmMap?.controller?.animateTo(it)
                            isFollowMode = true
                        }
                    }
                )

                FloatingTacticalAction(
                    icon = if (isFollowMode) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                    label = if (isFollowMode) "LOCK_ON" else "FOLLOW_ME",
                    onClick = { isFollowMode = !isFollowMode }
                )
                
                if (currentRouteOverlay != null) {
                    FloatingTacticalAction(
                        icon = Icons.Default.Close,
                        label = "CLEAR_ROUTE",
                        onClick = {
                            osmMap?.overlays?.remove(currentRouteOverlay)
                            currentRouteOverlay = null
                            osmMap?.invalidate()
                        }
                    )
                }
                
                FloatingTacticalAction(
                    icon = Icons.Default.HomeWork,
                    label = stringResource(R.string.action_nearest_base),
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val startPoint = locationOverlayState?.myLocation
                            if (startPoint == null) {
                                withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.msg_gps_waiting), Toast.LENGTH_SHORT).show() }
                                return@launch
                            }
                            val nearestCamp = camps.minByOrNull { camp -> startPoint.distanceToAsDouble(GeoPoint(camp.latitude, camp.longitude)) }
                            nearestCamp?.let { calculateAndDrawRoute(context, startPoint, GeoPoint(it.latitude, it.longitude)) { p ->
                                currentRouteOverlay?.let { osmMap?.overlays?.remove(it) }
                                currentRouteOverlay = p
                                osmMap?.overlays?.add(p)
                                val routePoints = p.points
                                if (routePoints.isNotEmpty()) {
                                    val box = BoundingBox.fromGeoPoints(routePoints)
                                    osmMap?.zoomToBoundingBox(box, true, 150)
                                    isFollowMode = false
                                }
                                osmMap?.invalidate()
                            }}
                        }
                    }
                )
                FloatingTacticalAction(
                    icon = Icons.Default.PersonSearch,
                    label = stringResource(R.string.action_nearest_peer),
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val startPoint = locationOverlayState?.myLocation
                            if (startPoint == null) {
                                withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.msg_gps_waiting), Toast.LENGTH_SHORT).show() }
                                return@launch
                            }
                            val nearestPeer = peers.values
                                .filter { it.latitude != null }
                                .sortedByDescending { it.messageType == "SOS" }
                                .minByOrNull { peer -> startPoint.distanceToAsDouble(GeoPoint(peer.latitude!!, peer.longitude!!)) }

                            nearestPeer?.let { calculateAndDrawRoute(context, startPoint, GeoPoint(it.latitude!!, it.longitude!!)) { p ->
                                currentRouteOverlay?.let { osmMap?.overlays?.remove(it) }
                                currentRouteOverlay = p
                                osmMap?.overlays?.add(p)
                                val routePoints = p.points
                                if (routePoints.isNotEmpty()) {
                                    val box = BoundingBox.fromGeoPoints(routePoints)
                                    osmMap?.zoomToBoundingBox(box, true, 150)
                                    isFollowMode = false
                                }
                                osmMap?.invalidate()
                            }}
                        }
                    }
                )
            }

            if (sosPeers.isNotEmpty()) {
                val latestSos = sosPeers.last()
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .width(200.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("SOS_SIGNAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(latestSos.senderName, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val startPoint = locationOverlayState?.myLocation
                                    if (startPoint == null) return@launch
                                    val target = GeoPoint(latestSos.latitude!!, latestSos.longitude!!)
                                    calculateAndDrawRoute(context, startPoint, target) { p ->
                                        currentRouteOverlay?.let { osmMap?.overlays?.remove(it) }
                                        currentRouteOverlay = p
                                        osmMap?.overlays?.add(p)
                                        val routePoints = p.points
                                        if (routePoints.isNotEmpty()) {
                                            val box = BoundingBox.fromGeoPoints(routePoints)
                                            osmMap?.zoomToBoundingBox(box, true, 150)
                                            isFollowMode = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("RESCUE", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampScreen(viewModel: BluetoothViewModel, onBack: () -> Unit) {
    val camps by viewModel.camps.collectAsState()
    var campName by remember { mutableStateOf("") }
    var campLat by remember { mutableStateOf("") }
    var campLon by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.header_base_stations), style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.AddHome, contentDescription = stringResource(R.string.cd_add_base), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (camps.isEmpty()) {
                EmptyBaseState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(camps) { camp ->
                        BaseStationCard(camp)
                    }
                }
            }
        }
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.title_establish_base), style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = campName, 
                            onValueChange = { campName = it }, 
                            label = { Text(stringResource(R.string.label_base_name)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = campLat, 
                            onValueChange = { campLat = it }, 
                            label = { Text(stringResource(R.string.label_latitude)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = campLon, 
                            onValueChange = { campLon = it }, 
                            label = { Text(stringResource(R.string.label_longitude)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val lat = campLat.toDoubleOrNull()
                            val lon = campLon.toDoubleOrNull()
                            if (campName.isNotBlank() && lat != null && lon != null) {
                                viewModel.saveCamp(campName, lat, lon)
                                showAddDialog = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(stringResource(R.string.action_broadcast)) }
                },
                dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
            )
        }
    }
}

@Composable
fun BaseStationCard(camp: Camp) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(camp.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.label_coords_format, camp.latitude, camp.longitude), 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* Navigate logic if needed */ }) {
                Icon(Icons.Default.Navigation, contentDescription = stringResource(R.string.cd_navigate), tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun EmptyBaseState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.HolidayVillage, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.msg_no_base_found), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun FloatingTacticalAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NetworkStatusDashboard(peerTelemetry: Map<String, BluetoothMessage>, onExpand: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onExpand() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyberGreen))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.header_mesh_comm), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(peerTelemetry.values.toList()) { telemetry ->
                PeerTelemetryCard(telemetry)
            }
        }
    }
}

@Composable
fun PeerTelemetryCard(telemetry: BluetoothMessage) {
    val isSOS = telemetry.messageType == "SOS"
    Card(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSOS) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isSOS) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(telemetry.senderName.uppercase(), style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSOS) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.label_battery_percentage, telemetry.batteryPercentage ?: 0), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(
                    if (telemetry.latitude != null) stringResource(R.string.label_coords_format, "%.3f".format(telemetry.latitude), "%.3f".format(telemetry.longitude)) else stringResource(R.string.label_unknown),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: BluetoothMessage, isFromMe: Boolean) {
    val alignment = if (isFromMe) Alignment.End else Alignment.Start
    val shape = if (isFromMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }
    val containerColor = if (isFromMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val contentColor = if (isFromMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isFromMe) {
            Text(
                message.senderName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                letterSpacing = 1.sp
            )
        } else {
            Text(
                stringResource(R.string.label_sender_me).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                letterSpacing = 1.sp
            )
        }
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            border = if (isFromMe) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    message.message,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                    if (isFromMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(viewModel: BluetoothViewModel, onBack: () -> Unit) {
    val peerTelemetry by viewModel.peerTelemetry.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_peer_locations)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            items(peerTelemetry.values.toList()) { telemetry ->
                LocationPeerItem(telemetry)
            }
        }
    }
}

@Composable
fun LocationPeerItem(telemetry: BluetoothMessage) {
    ListItem(
        headlineContent = { Text(telemetry.senderName) },
        supportingContent = { Text("${telemetry.latitude ?: "0.0"}, ${telemetry.longitude ?: "0.0"}") },
        leadingContent = { Icon(Icons.Default.PersonPinCircle, contentDescription = null) }
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    ) { innerPadding ->
        Text(stringResource(R.string.msg_settings_screen), modifier = Modifier.padding(innerPadding).padding(16.dp))
    }
}

fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

suspend fun calculateAndDrawRoute(
    context: android.content.Context,
    start: GeoPoint,
    end: GeoPoint,
    onResult: (Polyline) -> Unit
) {
    val roadManager: RoadManager = OSRMRoadManager(context, "Raven/1.0")
    val waypoints = arrayListOf(start, end)
    
    val road = withContext(Dispatchers.IO) {
        try {
            roadManager.getRoad(waypoints)
        } catch (e: Exception) {
            null
        }
    }

    withContext(Dispatchers.Main) {
        val polyline: Polyline
        if (road != null && road.mStatus == Road.STATUS_OK) {
            polyline = RoadManager.buildRoadOverlay(road)
            polyline.outlinePaint.strokeWidth = 12f
            polyline.outlinePaint.color = "#00FF41".toColorInt() // Cyber Green
            Toast.makeText(context, context.getString(R.string.msg_route_calculated, road.mLength), Toast.LENGTH_SHORT).show()
        } else {
            polyline = Polyline()
            polyline.setPoints(listOf(start, end))
            polyline.outlinePaint.strokeWidth = 10f
            polyline.outlinePaint.color = "#FFFF5F1F".toColorInt() // Neon Orange
            polyline.outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(30f, 15f), 0f)
            Toast.makeText(context, context.getString(R.string.msg_offline_fallback), Toast.LENGTH_LONG).show()
        }
        onResult(polyline)
    }
}
