package com.spidey.js.angad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spidey.js.angad.ui.theme.*
import com.spidey.js.angad.util.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefManager = remember { PreferencesManager(context) }
    val sensitivity by prefManager.sensitivityThreshold.collectAsState(initial = 0.75f)
    val notificationsEnabled by prefManager.notificationsEnabled.collectAsState(initial = true)
    val autoRestart by prefManager.autoRestart.collectAsState(initial = true)

    Box(modifier = Modifier.fillMaxSize().background(DeepEarth)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text(
                "SHIELD CONFIG",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 28.sp, fontWeight = FontWeight.Black, color = RoyalGold, letterSpacing = 2.sp
                )
            )
            Text("FINE-TUNE YOUR PROTECTION", style = MaterialTheme.typography.labelSmall, color = DivineSaffron)

            Spacer(modifier = Modifier.height(32.dp))

            SettingsSection("SECURITY MODEL") {
                Column {
                    Text("Phishing Detection Sensitivity", style = MaterialTheme.typography.titleMedium, color = AncientWhite)
                    Text("Higher sensitivity may increase false positives", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Slider(
                        value = sensitivity,
                        onValueChange = { scope.launch { prefManager.setSensitivity(it) } },
                        colors = SliderDefaults.colors(
                            thumbColor = RoyalGold,
                            activeTrackColor = RoyalGold,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "${(sensitivity * 100).toInt()}% Confidence Threshold",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End),
                        color = RoyalGold
                    )
                }
            }

            SettingsSection("PREFERENCES") {
                SettingsToggleItem("Real-time Notifications", "Alert on threat detection", notificationsEnabled) {
                    scope.launch { prefManager.setNotificationsEnabled(it) }
                }
                SettingsToggleItem("Auto-restart on Boot", "Keep protection active after restart", autoRestart) {
                    scope.launch { prefManager.setAutoRestart(it) }
                }
            }

            var showClearDialog by remember { mutableStateOf(false) }
            val database = remember { com.spidey.js.angad.db.AngadDatabase.getDatabase(context) }
            var resetSuccess by remember { mutableStateOf(false) }

            SettingsSection("DATA & PRIVACY") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic 7-Day Cleanup", style = MaterialTheme.typography.titleMedium, color = AncientWhite)
                            Text("Logs older than 7 days are auto-purged from phone storage", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Text("ACTIVE", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LavaCrimson.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LavaCrimson.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = LavaCrimson, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLEAR ALL LOGS & RESET DATA", color = LavaCrimson, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (resetSuccess) {
                        Text(
                            text = "✓ All logs cleared and reset to 0",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Reset All Logs & Traffic Data?", color = AncientWhite, fontWeight = FontWeight.Bold) },
                    text = { Text("This will permanently delete all recorded DNS events, blocks, and counters from your device storage and reset everything to 0.", color = TextMuted) },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    database.dnsEventDao().clearAll()
                                    resetSuccess = true
                                    showClearDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LavaCrimson)
                        ) {
                            Text("RESET TO 0", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("CANCEL", color = TextMuted)
                        }
                    },
                    containerColor = TempleSurface,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            SettingsSection("SYSTEM MODELS") {
                SettingsItem("Network Guard", "CICIDS2017 Deep Classifier (M1)")
                SettingsItem("ShieldNet AI", "ShieldNet Quantized Dynamic (M2)")
                SettingsItem("PayloadNet", "DNS Byte Pattern Classifier (M3)")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "ANGAD Dharma Protection Engine v1.0.0", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally), color = TextMuted)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = RoyalGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = TempleSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = AncientWhite)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedGold)
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = AncientWhite)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RoyalGold,
                checkedTrackColor = RoyalGold.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.DarkGray,
                uncheckedTrackColor = Color.Black
            )
        )
    }
}
