package com.spidey.js.angad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spidey.js.angad.db.AngadDatabase
import com.spidey.js.angad.db.DnsEvent
import com.spidey.js.angad.ui.components.DivineBackground
import com.spidey.js.angad.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class TimeFilter(val label: String, val durationMs: Long) {
    ONE_HOUR("1 HOUR", 60 * 60 * 1000L),
    TWENTY_FOUR_HOURS("24 HOURS", 24 * 60 * 60 * 1000L),
    SEVEN_DAYS("7 DAYS", 7 * 24 * 60 * 60 * 1000L),
    ALL("ALL", Long.MAX_VALUE)
}

data class AppTrafficGroup(
    val appLabel: String,
    val appPackage: String,
    val events: List<DnsEvent>,
    val threatCount: Int,
    val totalCount: Int,
    val latestTimestamp: Long
)

@Composable
fun TrafficMonitorScreen() {
    val context = LocalContext.current
    val database = AngadDatabase.getDatabase(context)
    val allDnsEvents by database.dnsEventDao().getAllEvents().collectAsState(initial = emptyList())

    var selectedEvent by remember { mutableStateOf<DnsEvent?>(null) }
    var selectedAppGroup by remember { mutableStateOf<AppTrafficGroup?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TimeFilter.TWENTY_FOUR_HOURS) }

    // Filter events by time range
    val now = remember(allDnsEvents) { System.currentTimeMillis() }
    val filteredEvents = remember(allDnsEvents, selectedFilter, searchQuery) {
        val cutoff = if (selectedFilter == TimeFilter.ALL) 0L else (now - selectedFilter.durationMs)
        allDnsEvents
            .filter { it.timestamp >= cutoff }
            .filter {
                if (searchQuery.isBlank()) true
                else {
                    it.appLabel.contains(searchQuery, ignoreCase = true) ||
                    it.domain.contains(searchQuery, ignoreCase = true) ||
                    it.appPackage.contains(searchQuery, ignoreCase = true)
                }
            }
    }

    // Group by App and sort by MOST RECENT ACTIVITY TIMESTAMP (active apps rise to the top!)
    val appGroups = remember(filteredEvents) {
        filteredEvents
            .groupBy { it.appLabel }
            .map { (appLabel, events) ->
                val latestTime = events.maxOfOrNull { it.timestamp } ?: 0L
                AppTrafficGroup(
                    appLabel = appLabel,
                    appPackage = events.first().appPackage,
                    events = events.sortedByDescending { it.timestamp },
                    threatCount = events.count { it.isThreat },
                    totalCount = events.size,
                    latestTimestamp = latestTime
                )
            }
            .sortedByDescending { it.latestTimestamp } // Recency-based sorting
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DivineBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    "NET-CHAKRA",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 26.sp, fontWeight = FontWeight.Black,
                        color = RoyalGold, letterSpacing = 2.sp
                    )
                )
                Text("FLOW OF DIGITAL ENERGY", style = MaterialTheme.typography.labelSmall, color = DivineSaffron)
            }

            // ── Activity Intensity & Filter Card (replaces redundant overview cards) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = TempleSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TRAFFIC INTENSITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGold,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE FEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-time Traffic Bars
                    val barCount = 14
                    val recentEvents = filteredEvents.take(barCount)
                    val barScores = recentEvents.map { it.riskScore.toFloat() }.let { list ->
                        if (list.size < barCount) list + List(barCount - list.size) { 0.05f } else list
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        barScores.reversed().forEach { score ->
                            val barColor = when {
                                score > 0.8f -> LavaCrimson
                                score > 0.35f -> DivineSaffron
                                else -> RoyalGold
                            }
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(maxOf(score, 0.12f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(barColor, barColor.copy(alpha = 0.35f))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimeFilter.values().forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) RoyalGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        1.dp,
                                        if (isSelected) RoyalGold else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedFilter = filter }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSelected) RoyalGold else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Top Threat Alert in selected range (if any)
                    val threatCount = filteredEvents.count { it.isThreat }
                    if (threatCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LavaCrimson.copy(alpha = 0.12f))
                                .border(1.dp, LavaCrimson.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = LavaCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$threatCount threat${if (threatCount > 1) "s" else ""} purged in ${selectedFilter.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LavaCrimson,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Search Bar ───────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search apps, domains...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TempleSurface,
                    unfocusedContainerColor = TempleSurface,
                    focusedBorderColor = RoyalGold.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedTextColor = AncientWhite,
                    unfocusedTextColor = AncientWhite
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Timeline Section Header ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(DivineSaffron)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "APPLICATION ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = DivineSaffron,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${filteredEvents.size} records",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Timeline Content with Continuous Line ────────────────
            if (filteredEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No traffic in selected range.", color = TextMuted)
                        Text(
                            "Try selecting 'ALL' or browse the web.",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Left Timeline Connector Line
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(DivineSaffron.copy(alpha = 0.8f), RoyalGold.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // App Group Cards List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(appGroups, key = { it.appLabel }) { group ->
                            AppGroupCard(group = group, onClick = { selectedAppGroup = group })
                        }
                    }
                }
            }
        }
    }

    // Dedicated Full-Screen Page for App Activity Details
    selectedAppGroup?.let { group ->
        AppDetailScreen(
            group = group,
            onBack = { selectedAppGroup = null },
            onEventClick = { selectedEvent = it }
        )
    }

    selectedEvent?.let {
        SecurityInsightSheet(event = it) { selectedEvent = null }
    }
}

// ── Dedicated App Detail Page ────────────────────────────────────────────────
@Composable
fun AppDetailScreen(
    group: AppTrafficGroup,
    onBack: () -> Unit,
    onEventClick: (DnsEvent) -> Unit
) {
    var appDomainSearch by remember { mutableStateOf("") }

    val filteredDomains = remember(group.events, appDomainSearch) {
        group.events
            .groupBy { it.domain }
            .map { (_, eventsForDomain) ->
                eventsForDomain.maxByOrNull { it.timestamp }!! to eventsForDomain.size
            }
            .filter { (event, _) ->
                if (appDomainSearch.isBlank()) true
                else event.domain.contains(appDomainSearch, ignoreCase = true)
            }
            .sortedByDescending { (event, _) -> if (event.isThreat) 1 else 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DivineBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TempleSurface)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = RoyalGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.appLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AncientWhite
                    )
                    Text(
                        text = group.appPackage,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Metrics Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TempleSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = group.totalCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = RoyalGold
                        )
                        Text(
                            text = "TOTAL QUERIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = group.threatCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (group.threatCount > 0) LavaCrimson else Color(0xFF4CAF50)
                        )
                        Text(
                            text = "THREATS PURGED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = filteredDomains.size.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DivineSaffron
                        )
                        Text(
                            text = "UNIQUE DOMAINS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Domain Search in App
            OutlinedTextField(
                value = appDomainSearch,
                onValueChange = { appDomainSearch = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter domains called by ${group.appLabel}...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (appDomainSearch.isNotEmpty()) {
                        IconButton(onClick = { appDomainSearch = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TempleSurface,
                    unfocusedContainerColor = TempleSurface,
                    focusedBorderColor = RoyalGold.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedTextColor = AncientWhite,
                    unfocusedTextColor = AncientWhite
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "REQUEST LOGS (${filteredDomains.size})",
                style = MaterialTheme.typography.labelSmall,
                color = RoyalGold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredDomains.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching domains found.", color = TextMuted)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TempleSurface.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredDomains) { (event, count) ->
                            DomainEventRow(event = event, count = count, onClick = { onEventClick(event) })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        }
                    }
                }
            }
        }
    }
}

// ── App Group Card (Phonebook Style) ───────────────────────────────────────────
@Composable
fun AppGroupCard(group: AppTrafficGroup, onClick: () -> Unit) {
    val latestTimeStr = remember(group.latestTimestamp) {
        if (group.latestTimestamp > 0) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(group.latestTimestamp))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = TempleSurface.copy(0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = if (group.threatCount > 0) androidx.compose.foundation.BorderStroke(1.dp, LavaCrimson.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val appColor = when {
                group.threatCount > 0 -> LavaCrimson
                else -> RoyalGold
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(appColor.copy(alpha = 0.15f))
                    .border(1.dp, appColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.appLabel.firstOrNull()?.uppercase() ?: "?",
                    color = appColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AncientWhite
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (group.threatCount > 0) {
                        Text(
                            text = "${group.threatCount} threat${if (group.threatCount > 1) "s" else ""} blocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavaCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(" · ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Text(
                        text = "${group.totalCount} connection${if (group.totalCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (group.threatCount > 0) LavaCrimson.copy(0.7f) else TextMuted
                    )
                    if (latestTimeStr.isNotBlank()) {
                        Text(" · ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = latestTimeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = RoyalGold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Domain Row inside App Card ────────────────────────────────────────────────
@Composable
fun DomainEventRow(event: DnsEvent, count: Int, onClick: () -> Unit) {
    val isThreat = event.isThreat
    val riskColor = when {
        isThreat -> LavaCrimson
        event.riskScore > 0.35 -> DivineSaffron
        else -> Color(0xFF4CAF50)
    }

    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
    val ipStr = remember(event.aiMetadata) {
        event.aiMetadata?.split(";")?.firstOrNull { it.startsWith("IP=") }?.removePrefix("IP=") ?: ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(riskColor.copy(alpha = 0.8f))
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.domain,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = AncientWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isThreat) LavaCrimson.copy(0.8f) else TextMuted,
                    fontSize = 10.sp
                )
                if (ipStr.isNotBlank() && ipStr != "0.0.0.0") {
                    Text(" · ", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = ipStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = AncientWhite.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
                Text(" · ", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                Text(
                    text = event.queryType,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        // Connection count badge (e.g. 2x, 3x)
        if (count > 1) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${count}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Status indicator Icon (Blocked red / Clean green)
        if (isThreat) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(LavaCrimson.copy(alpha = 0.12f))
                    .border(1.dp, LavaCrimson.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = "Threat Blocked",
                    tint = LavaCrimson,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Clean Traffic",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
