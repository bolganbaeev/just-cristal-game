package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CrystalViolet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ToxicEmerald
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val playerName by viewModel.playerName.collectAsState()
    val highestWave by viewModel.highestWave.collectAsState(initial = null)
    val totalKills by viewModel.totalZombiesKilled.collectAsState(initial = null)

    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(playerName) }
    var isMuted by remember { mutableStateOf(viewModel.audio.isMuted()) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crystal_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        tempName = playerName
                        showNameDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("player_profile_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Name",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Top action buttons: Sound & Leaderboard
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val next = !isMuted
                        isMuted = next
                        viewModel.audio.setMuted(next)
                    },
                    modifier = Modifier.testTag("mute_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Toggle Audio",
                        tint = if (isMuted) TextMuted else NeonCyan
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.SCORES) },
                    modifier = Modifier.testTag("scores_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "High Scores",
                        tint = CrystalViolet
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Crystal Logo & Title
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.4f),
                                CrystalViolet.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.cristal_icon),
                contentDescription = "Cristal Zombie Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, NeonCyan, RoundedCornerShape(22.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CRISTAL ZOMBIE",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NeonCyan,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "LAN CO-OP DEFENSE • UNIVERSAL CROSSPLAY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CrystalViolet,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Mode Card: Host LAN Game (Primary Hero Action)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.prepareHostLobby() }
                .testTag("host_lan_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan, CrystalViolet)))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Host LAN",
                        tint = NeonCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HOST LAN GAME",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Play with iPhone, iPad & Web friends over local Wi-Fi with instant QR code join.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Card: Solo Defense
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.startSoloGame() }
                .testTag("solo_game_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(DarkSurfaceBorder, ToxicEmerald.copy(alpha = 0.4f))))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ToxicEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Solo Play",
                        tint = ToxicEmerald,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOLO DEFENSE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Defend the core alone. Survive increasing zombie waves and unlock upgrades.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Card: Join LAN Game
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(AppScreen.JOIN_LOBBY) }
                .testTag("join_game_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(DarkSurfaceBorder, CrystalViolet.copy(alpha = 0.4f))))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CrystalViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Join LAN",
                        tint = CrystalViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "JOIN LAN HOST",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Connect as a peer defender to a host on this Wi-Fi network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface.copy(alpha = 0.6f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RECORD WAVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text(
                    text = "${highestWave ?: 0}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ZOMBIES SLAIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text(
                    text = "${totalKills ?: 0}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ToxicEmerald
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PLATFORMS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text(
                    text = "Android • iOS • Web",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrystalViolet
                )
            }
        }
    }

    // Name Editor Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Defender Handle", color = NeonCyan) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it.take(14) },
                    label = { Text("Callsign") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("player_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.setPlayerName(tempName.trim())
                        }
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0A0E17)),
                    modifier = Modifier.testTag("save_name_btn")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}
