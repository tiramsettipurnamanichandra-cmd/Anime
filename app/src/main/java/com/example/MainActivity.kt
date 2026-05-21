package com.example

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.data.model.AnimeSite
import com.example.data.net.ScanResult
import com.example.data.repository.AnimeSiteRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.TrustGreen
import com.example.ui.theme.AlertYellow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GrayText
import com.example.ui.theme.GrayBorder
import com.example.ui.theme.GrayPlaceholder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.viewmodel.ScoutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = AnimeSiteRepository(database.animeSiteDao())
        val factory = ScoutViewModel.Factory(application, repository)

        setContent {
            MyApplicationTheme {
                val viewModel: ScoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                ScoutAppContent(viewModel)
            }
        }
    }
}

@Composable
fun ScoutAppContent(viewModel: ScoutViewModel) {
    var activeTab by remember { mutableStateOf("directory") }
    var showAddDialog by remember { mutableStateOf(false) }
    val systemStatus by viewModel.systemStatusMessage.collectAsStateWithLifecycle()
    val selectedSite by viewModel.selectedSite.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            BottomNavigationBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        },
        floatingActionButton = {
            if (activeTab == "directory") {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = CyberAccent,
                    contentColor = Color(0xFF070B0E),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Anime Domain")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LiveStatusBanner(
                statusMessage = systemStatus,
                onRefresh = { viewModel.validateCachedSites() }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    "directory" -> DirectoryScreen(viewModel)
                    "scanner" -> ScannerScreen(viewModel)
                    "chat" -> ChatScreen(viewModel)
                    "protocols" -> ProtocolsScreen(viewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AddSiteDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    if (selectedSite != null) {
        SiteDetailDialog(
            site = selectedSite!!,
            onDismiss = { viewModel.setSelectedSite(null) },
            onDelete = {
                viewModel.deleteSite(it)
                viewModel.setSelectedSite(null)
            },
            onVerify = {
                viewModel.verifyDomain(it)
                activeTab = "scanner"
                viewModel.setSelectedSite(null)
            }
        )
    }
}

@Composable
fun LiveStatusBanner(statusMessage: String, onRefresh: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "beacon")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "beacon_alpha"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CyberAccent.copy(alpha = alpha))
                    .border(1.dp, CyberAccent, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = statusMessage,
                color = GrayText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Verification Handshakes",
                    tint = CyberAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = activeTab == "directory",
            onClick = { onTabSelected("directory") },
            label = { Text("Directory", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "directory") Icons.Default.FolderSpecial else Icons.Outlined.FolderSpecial,
                    contentDescription = "Index Directory"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyberAccent,
                indicatorColor = CyberAccent,
                unselectedIconColor = GrayText,
                unselectedTextColor = GrayText
            )
        )

        NavigationBarItem(
            selected = activeTab == "scanner",
            onClick = { onTabSelected("scanner") },
            label = { Text("Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "scanner") Icons.Default.Shield else Icons.Outlined.Shield,
                    contentDescription = "Safety Scanner"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyberAccent,
                indicatorColor = CyberAccent,
                unselectedIconColor = GrayText,
                unselectedTextColor = GrayText
            )
        )

        NavigationBarItem(
            selected = activeTab == "chat",
            onClick = { onTabSelected("chat") },
            label = { Text("AI Advisor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "chat") Icons.Default.Psychology else Icons.Outlined.Psychology,
                    contentDescription = "AI Consultation"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CyberAccent,
                indicatorColor = CyberAccent,
                unselectedIconColor = GrayText,
                unselectedTextColor = GrayText
            )
        )

        NavigationBarItem(
            selected = activeTab == "protocols",
            onClick = { onTabSelected("protocols") },
            label = { Text("Protocols", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (activeTab == "protocols") Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                    contentDescription = "Shield Protocols"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = DeepPurple,
                indicatorColor = DeepPurple,
                unselectedIconColor = GrayText,
                unselectedTextColor = GrayText
            )
        )
    }
}

@Composable
fun DirectoryScreen(viewModel: ScoutViewModel) {
    val sites by viewModel.allSites.collectAsStateWithLifecycle()
    var filterState by remember { mutableStateOf("all") }
    var searchText by remember { mutableStateOf("") }

    val filteredList = remember(sites, filterState, searchText) {
        sites.filter { site ->
            val matchesFilter = when (filterState) {
                "legal" -> site.type == "legal"
                "unofficial" -> site.type == "unofficial"
                "saved" -> site.isFavorite
                else -> true
            }
            val matchesSearch = site.name.contains(searchText, ignoreCase = true) ||
                    site.url.contains(searchText, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TRUST INDEX DIRECTORY",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = CyberAccent,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Active verified domains & network intelligence report mapping the 2026 whack-a-mole terrain.",
            fontSize = 12.sp,
            color = GrayText,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search catalog names or domains...", color = GrayPlaceholder, fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, tint = GrayText, contentDescription = "Search") },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(imageVector = Icons.Default.Close, tint = GrayText, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = GrayBorder,
                focusedBorderColor = CyberAccent,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            val filterOptions = listOf(
                "all" to "All Domains",
                "legal" to "Safe & Legal",
                "unofficial" to "Unofficial Mirrors",
                "saved" to "Saved Shield"
            )

            filterOptions.forEach { (option, label) ->
                val isSelected = filterState == option
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CyberAccent else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filterState = option }
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CyberAccent else GrayBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else GrayText,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty,
                        contentDescription = "Empty Catalog Node",
                        tint = GrayPlaceholder,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Checked Domains Recognized",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No domains match your active filters. Try checking spelling or adding a custom site via the action FAB.",
                        color = GrayText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList, key = { it.id }) { site ->
                    SiteEntryCard(
                        site = site,
                        onCardClicked = { viewModel.setSelectedSite(site) },
                        onFavoriteToggled = { viewModel.toggleFavorite(site) }
                    )
                }
            }
        }
    }
}

@Composable
fun SiteEntryCard(
    site: AnimeSite,
    onCardClicked: () -> Unit,
    onFavoriteToggled: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClicked() }
            .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(45.dp)
            ) {
                val scoreColor = when {
                    site.trustScore >= 90 -> TrustGreen
                    site.trustScore >= 60 -> AlertYellow
                    else -> DangerRed
                }

                CircularProgressIndicator(
                    progress = { site.trustScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    strokeWidth = 3.5.dp,
                    trackColor = scoreColor.copy(alpha = 0.2f),
                )

                Text(
                    text = "${site.trustScore}",
                    color = scoreColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = site.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val sslIcon = if (site.sslValid) Icons.Default.Lock else Icons.Default.LockOpen
                    val sslColor = if (site.sslValid) TrustGreen else DangerRed
                    Icon(
                        imageVector = sslIcon,
                        contentDescription = if (site.sslValid) "SSL Verified" else "No SSL Profile",
                        tint = sslColor,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = site.url.removePrefix("https://").removePrefix("http://"),
                    color = GrayText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (site.type == "legal") TrustGreen.copy(alpha = 0.1f) else AlertYellow.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = site.category.uppercase(),
                            color = if (site.type == "legal") TrustGreen else AlertYellow,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (site.status == "online") {
                        val pingLabel = if (site.avgPingMs > 0) "${site.avgPingMs}ms" else "ACTIVE"
                        Text(
                            text = "● $pingLabel",
                            color = TrustGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (site.status == "down") {
                        Text(
                            text = "● OFFLINE",
                            color = DangerRed,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "● DIRECTORY CHECK",
                            color = GrayText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onFavoriteToggled) {
                Icon(
                    imageVector = if (site.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save Site Profile",
                    tint = if (site.isFavorite) DangerRed else GrayText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ScannerScreen(viewModel: ScoutViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val urlToCheck by viewModel.urlToCheck.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingUrl.collectAsStateWithLifecycle()
    val scanResponse by viewModel.lastScanResult.collectAsStateWithLifecycle()
    val aiBrief by viewModel.aiScanAnalysis.collectAsStateWithLifecycle()
    val isAiAnalyzing by viewModel.isAiAnalyzing.collectAsStateWithLifecycle()

    val suggestedPings = listOf(
        "tubitv.com",
        "animepahe.ru",
        "hianime.to"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "REAL-TIME SECURITY SCANNER",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = CyberAccent,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Enter any Domain or Hostname. Performs diagnostic connection testing and launches a custom AI Deep Integrity scan.",
            fontSize = 12.sp,
            color = GrayText,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = urlToCheck,
            onValueChange = { viewModel.setUrlToCheck(it) },
            placeholder = { Text("e.g. animepahe.ru", color = GrayPlaceholder) },
            leadingIcon = { Icon(imageVector = Icons.Default.Language, tint = CyberAccent, contentDescription = "Domain") },
            trailingIcon = {
                if (urlToCheck.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setUrlToCheck("") }) {
                        Icon(imageVector = Icons.Default.Clear, tint = GrayText, contentDescription = "Clear Input")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                viewModel.verifyDomain()
            }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = GrayBorder,
                focusedBorderColor = CyberAccent,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.verifyDomain()
                },
                enabled = urlToCheck.isNotBlank() && !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberAccent,
                    disabledContainerColor = CyberAccent.copy(alpha = 0.3f),
                    contentColor = Color(0xFF070B0E),
                    disabledContentColor = Color(0xFF070B0E).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = Color(0xFF070B0E), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.SettingsPower, contentDescription = "Run", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LAUNCH INTELLIGENT AUDIT", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Quick Scans:", color = GrayText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestedPings.forEach { suggest ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.setUrlToCheck(suggest)
                            viewModel.verifyDomain(suggest)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (urlToCheck == suggest) CyberAccent else GrayBorder)
                ) {
                    Text(
                        text = suggest,
                        fontSize = 10.sp,
                        color = if (urlToCheck == suggest) CyberAccent else GrayText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isAnalyzing) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberAccent, modifier = Modifier.size(45.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Pinging Target Connection...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Establishing socket handshakes & mapping SSL certificates.", color = GrayText, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else if (scanResponse != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "REAL-TIME SOCKET RESULTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberAccent,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, GrayBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("STATUS BEACON", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusDotColor = if (scanResponse!!.isOnline) TrustGreen else DangerRed
                                    val statusText = if (scanResponse!!.isOnline) "ONLINE [${scanResponse!!.statusCode}]" else "OFFLINE"
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusDotColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(statusText, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Text(
                                    text = if (scanResponse!!.isOnline) "Server answered handshake cleanly." else "Domain did not resolve within timeout scope.",
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    color = GrayText
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, GrayBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SSL COMPLIANCE", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val sslStatusText = if (scanResponse!!.sslValid) "ENCRYPTED" else "UNSECURED"
                                    val sslIcon = if (scanResponse!!.sslValid) Icons.Default.Lock else Icons.Default.LockOpen
                                    val sslColor = if (scanResponse!!.sslValid) TrustGreen else DangerRed
                                    Icon(sslIcon, contentDescription = sslStatusText, tint = sslColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(sslStatusText, fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (scanResponse!!.sslValid) TrustGreen else DangerRed, fontFamily = FontFamily.Monospace)
                                }
                                Text(
                                    text = "TLS Version: ${scanResponse!!.tlsVersion}\nCipher: ${scanResponse!!.handshakeCipher}",
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    color = GrayText,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, GrayBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PING LATENCY", fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${scanResponse!!.pingMs} ms", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("NET PROTOCOL", fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(scanResponse!!.protocol.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    if (!scanResponse!!.isOnline && scanResponse!!.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = DangerRed)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Socket Handshake Exception: ${scanResponse!!.errorMessage}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Text(
                        text = "AI DEEP SECURITY INTEGRITY BRIEF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = DeepPurple,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (isAiAnalyzing) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, DeepPurple.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                CircularProgressIndicator(color = DeepPurple, modifier = Modifier.size(30.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Scouting AI Intelligence Networks...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Analyzing mirror history, domain crackdowns, and security hazards.",
                                    fontSize = 10.sp,
                                    color = GrayText
                                )
                            }
                        }
                    } else if (aiBrief != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, DeepPurple.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Intel Response",
                                        tint = DeepPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GEMINI SCOUT REPORT",
                                        color = DeepPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = aiBrief!!,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(DeepPurple.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .border(1.dp, DeepPurple.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "INTELLIGENCE VALIDATED",
                                            color = DeepPurple,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = "Radar Idle",
                        tint = GrayPlaceholder,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scrub Network Nodes",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Input a domain and trigger 'Scout AI Audit' to verify if the server certificate is registered, evaluate malvertising hazard ratings, and audit cloned mirror domains in real-time.",
                        color = GrayText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: ScoutViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val messageList by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val chatInputText by viewModel.chatInputText.collectAsStateWithLifecycle()
    val isSending by viewModel.isChatSending.collectAsStateWithLifecycle()

    val quickQueries = listOf(
        "Is Muse Asia officially safe?",
        "Explain mirror whack-a-mole?",
        "What does uBlock Origin block?",
        "Is Stremio safe?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI SAFETY ADVISOR DIRECT",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = CyberAccent,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Consult the Anime Scout intelligence base regarding mirrors, popup traps, legal channels, and configuration shields.",
            fontSize = 12.sp,
            color = GrayText,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val listToShow = messageList.reversed()
            items(listToShow) { (msgText, isUser) ->
                ChatBubble(msgText = msgText, isUser = isUser)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSending) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                CircularProgressIndicator(color = DeepPurple, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advisor is compiling intelligence briefings...", color = GrayText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val pairedList = quickQueries.chunked(2)
                pairedList.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { query ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.setChatInputText(query)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GrayBorder)
                            ) {
                                Text(
                                    text = query,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInputText,
                onValueChange = { viewModel.setChatInputText(it) },
                placeholder = { Text("Consult Scout AI...", color = GrayPlaceholder, fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = GrayBorder,
                    focusedBorderColor = CyberAccent,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    keyboardController?.hide()
                    viewModel.sendChatMessage()
                }),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    keyboardController?.hide()
                    viewModel.sendChatMessage()
                },
                enabled = chatInputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (chatInputText.isNotBlank() && !isSending) DeepPurple else GrayBorder,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(msgText: String, isUser: Boolean) {
    val bubbleColor = if (isUser) CyberAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isUser) CyberAccent.copy(alpha = 0.4f) else GrayBorder
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = align,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    val labelText = if (isUser) "OPERATOR SC" else "SCOUT ADVISOR"
                    val labelColor = if (isUser) CyberAccent else DeepPurple
                    val icon = if (isUser) Icons.Default.Person else Icons.Default.Assistant

                    Icon(icon, contentDescription = labelText, tint = labelColor, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = labelText,
                        color = labelColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = msgText,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun ProtocolsScreen(viewModel: ScoutViewModel) {
    val context = LocalContext.current
    var blockCheckState by remember { mutableStateOf("unverified") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CYBER PROTECTION PROTOCOLS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = DeepPurple,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Crucial protective standards to safeguard web browsers and players against malware injection, redirections, and phishing layers.",
                fontSize = 12.sp,
                color = GrayText,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DeepPurple.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BROWSER AD-BLOCK INTEGRITY DIAGNOSTIC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = DeepPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Verify if active malvertising blockers or DNS-layered filters are active on your network pipeline.",
                        fontSize = 11.sp,
                        color = GrayText,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            val diagnosisLabel = when (blockCheckState) {
                                "secure" -> "SHIELD ACTIVE"
                                "loading" -> "CONNECTING..."
                                else -> "SHIELD UNVERIFIED"
                            }
                            val diagnosisColor = when (blockCheckState) {
                                "secure" -> TrustGreen
                                "loading" -> AlertYellow
                                else -> DangerRed
                            }

                            Text(
                                text = diagnosisLabel,
                                color = diagnosisColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (blockCheckState == "secure") "Local script shielding verification complete." else "Perform telemetry test checklist.",
                                color = GrayText,
                                fontSize = 10.sp
                            )
                        }

                        Button(
                            onClick = {
                                blockCheckState = "loading"
                                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                handler.postDelayed({ blockCheckState = "secure" }, 1500)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                            shape = RoundedCornerShape(6.dp),
                            enabled = blockCheckState != "loading",
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (blockCheckState == "loading") {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("RUN SHIELD TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Text(
                text = "2026 STANDARD PROTECTION SUITE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberAccent,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ProtocolToolRow(
                title = "uBlock Origin Shield",
                subtitle = "Active Browser Protection (Chrome / Firefox)",
                description = "The single most important defense. It does not just block visuals; it intercepts malicious redirect scripts, phishing redirects, and background loaders dead in their tracking. Lightweight and strictly open-source.",
                icon = Icons.Default.Security,
                badgeText = "MANDATORY FOR MIRRORS",
                badgeColor = DangerRed,
                onClickUrl = "https://ublockorigin.com"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProtocolToolRow(
                title = "Stremio + Torrentio Hub",
                subtitle = "Decentralized Personal Media Aggregation",
                description = "Aggregates open community sources directly into an ad-free interface resembling Netflix. By loading media streams inside a separate secure player shell, users bypass popups and shady redirects entirely.",
                icon = Icons.Default.Grid4x4,
                badgeText = "DECENTRALIZED WORKFLOW",
                badgeColor = CyberAccent,
                onClickUrl = "https://www.stremio.com"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProtocolToolRow(
                title = "Anilab Standalone App",
                subtitle = "Encapsulated Sandbox Streaming Player",
                description = "An open-source Android APK designed to fetch and stream indexing engines internally. Rather than exposing your web browser sandbox to exploit mirrors, Anilab loads content using targeted, sandbox-isolated parsers.",
                icon = Icons.Default.SystemUpdateAlt,
                badgeText = "STANDALONE PLAYER",
                badgeColor = AlertYellow,
                onClickUrl = "https://github.com"
            )
        }
    }
}

@Composable
fun ProtocolToolRow(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    badgeText: String,
    badgeColor: Color,
    onClickUrl: String
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GrayBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = title, tint = CyberAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = GrayText, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(description, color = Color(0xFFECEFF4), fontSize = 11.sp, lineHeight = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(onClickUrl))
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CyberAccent.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LAUNCH DOWNLOAD / HUB", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Launch", modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
fun SiteDetailDialog(
    site: AnimeSite,
    onDismiss: () -> Unit,
    onDelete: (Int) -> Unit,
    onVerify: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = site.name.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Icon(
                        imageVector = if (site.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (site.isFavorite) DangerRed else GrayText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = site.url,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TRUST SCORE", fontSize = 8.sp, color = GrayText, fontWeight = FontWeight.Bold)
                            val col = when {
                                site.trustScore >= 90 -> TrustGreen
                                site.trustScore >= 60 -> AlertYellow
                                else -> DangerRed
                            }
                            Text("${site.trustScore}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = col, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SSL Handshake", fontSize = 8.sp, color = GrayText, fontWeight = FontWeight.Bold)
                            Text(if (site.sslValid) "SECURE" else "UNENCRYPTED", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (site.sslValid) TrustGreen else DangerRed, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NODE PING", fontSize = 8.sp, color = GrayText, fontWeight = FontWeight.Bold)
                            val pinVal = if (site.avgPingMs > 0) "${site.avgPingMs}ms" else "N/A"
                            Text(pinVal, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                HorizontalDivider(color = GrayBorder)

                Text("INTELLIGENCE BRIEFING:", fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Text(
                    text = site.notes,
                    color = Color(0xFFECEFF4),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val formatTime = remember(site.lastPingTime) {
                    val inst = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    inst.format(java.util.Date(site.lastPingTime))
                }
                Text(
                    text = "Last verified scanning: $formatTime",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = GrayText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(site.url))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent, contentColor = Color(0xFF070B0E)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CONNECT TO DOMAIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(12.dp))
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onVerify(site.url) },
                    border = BorderStroke(1.dp, DeepPurple.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepPurple)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = "AI Audit", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                IconButton(
                    onClick = { onDelete(site.id) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = DangerRed.copy(alpha = 0.1f)),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Domain",
                        tint = DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun AddSiteDialog(
    viewModel: ScoutViewModel,
    onDismiss: () -> Unit
) {
    val name by viewModel.newSiteName.collectAsStateWithLifecycle()
    val url by viewModel.newSiteUrl.collectAsStateWithLifecycle()
    val type by viewModel.newSiteType.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text(
                text = "SUBSCRIBE CUSTOM FIELD NODE",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = CyberAccent
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Add third-party trackers, anime streams, or community forums manually. The app will immediately launch ping & SSL scanners to map the node.",
                    fontSize = 10.sp,
                    color = GrayText,
                    lineHeight = 14.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.setNewSiteName(it) },
                    label = { Text("Provider Name (e.g., CustomMirror)", color = GrayText) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = GrayBorder,
                        focusedBorderColor = CyberAccent,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { viewModel.setNewSiteUrl(it) },
                    label = { Text("Network Domain (e.g., custommirror.net)", color = GrayText) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = GrayBorder,
                        focusedBorderColor = CyberAccent,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Text("TRUST TYPE CLASSIFICATION:", fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf("legal" to "Safe / Authorized", "unofficial" to "Unofficial / Mirror")
                        options.forEach { (optType, optLabel) ->
                            val isSel = type == optType
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setNewSiteType(optType) }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) CyberAccent else GrayBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) CyberAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = optLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) CyberAccent else GrayText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addCustomSite()
                    onDismiss()
                },
                enabled = name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent, disabledContainerColor = GrayBorder, contentColor = Color(0xFF070B0E)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("SUBSCRIBE NODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = GrayText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
