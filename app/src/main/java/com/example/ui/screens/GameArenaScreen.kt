package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameSimulation
import com.example.model.UpgradeType
import com.example.model.WaveStatus
import com.example.model.ZombieType
import com.example.ui.components.VirtualJoystick
import com.example.ui.theme.CrystalViolet
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ToxicEmerald
import com.example.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameArenaScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val isGameOver by viewModel.isGameOver.collectAsState()
    val lastRecord by viewModel.lastMatchRecord.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var renderTick by remember { mutableFloatStateOf(0f) }

    // Repaint trigger synced to frame clock
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                renderTick += 0.016f
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "crystal_anim")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_size"
    )

    BackHandler {
        showExitDialog = true
    }

    val sim = viewModel.simulation
    val localPlayer = sim.players.find { it.id == "host_local" } ?: sim.players.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Main Game Arena Canvas
        Canvas(modifier = Modifier.fillMaxSize().testTag("game_arena_canvas")) {
            val canvasW = size.width
            val canvasH = size.height

            // Camera translation: center camera on local player
            val camX = localPlayer?.x ?: 600f
            val camY = localPlayer?.y ?: 600f
            val scale = (minOf(canvasW, canvasH) / 520f).coerceIn(0.6f, 1.4f)

            val offsetX = canvasW / 2f - camX * scale
            val offsetY = canvasH / 2f - camY * scale

            // 1. Draw Grid Background
            val gridSize = 80f * scale
            val startX = (offsetX % gridSize)
            val startY = (offsetY % gridSize)

            var x = startX
            while (x < canvasW) {
                drawLine(
                    color = DarkSurfaceBorder.copy(alpha = 0.25f),
                    start = Offset(x, 0f),
                    end = Offset(x, canvasH),
                    strokeWidth = 1f
                )
                x += gridSize
            }
            var y = startY
            while (y < canvasH) {
                drawLine(
                    color = DarkSurfaceBorder.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(canvasW, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }

            // 2. Draw Arena Boundaries
            drawRect(
                color = NeonCyan.copy(alpha = 0.4f),
                topLeft = Offset(offsetX, offsetY),
                size = Size(GameSimulation.ARENA_WIDTH * scale, GameSimulation.ARENA_HEIGHT * scale),
                style = Stroke(width = 3.dp.toPx())
            )

            // 3. Draw Central Crystal Core
            val cX = offsetX + sim.crystal.x * scale
            val cY = offsetY + sim.crystal.y * scale
            val cRadius = sim.crystal.radius * scale * pulseSize

            // Crystal glowing aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = if (sim.crystal.lastHitFlash > 0) 0.8f else 0.45f),
                        CrystalViolet.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(cX, cY),
                    radius = cRadius * 2.8f
                ),
                radius = cRadius * 2.8f,
                center = Offset(cX, cY)
            )

            // Crystal polygon
            val crystalPath = Path().apply {
                moveTo(cX, cY - cRadius)
                lineTo(cX + cRadius, cY)
                lineTo(cX, cY + cRadius)
                lineTo(cX - cRadius, cY)
                close()
            }
            drawPath(
                path = crystalPath,
                color = if (sim.crystal.lastHitFlash > 0) DangerRed else NeonCyan
            )
            // Inner faceted highlight
            val facetPath = Path().apply {
                moveTo(cX, cY - cRadius)
                lineTo(cX + cRadius * 0.45f, cY)
                lineTo(cX, cY + cRadius * 0.45f)
                close()
            }
            drawPath(path = facetPath, color = Color.White.copy(alpha = 0.7f))

            // Crystal Energy Shield Arc
            if (sim.crystal.shield > 0) {
                drawCircle(
                    color = CrystalViolet.copy(alpha = 0.45f),
                    radius = cRadius * 1.5f,
                    center = Offset(cX, cY),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 4. Draw Shards
            for (s in sim.shards) {
                val sX = offsetX + s.x * scale
                val sY = offsetY + s.y * scale
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 5.dp.toPx(),
                    center = Offset(sX, sY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(sX, sY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 5. Draw Projectiles
            for (p in sim.projectiles) {
                val pX = offsetX + p.x * scale
                val pY = offsetY + p.y * scale
                val pColor = if (p.isEnemy) DangerRed else NeonCyan
                drawCircle(
                    color = pColor,
                    radius = p.radius * scale,
                    center = Offset(pX, pY)
                )
                drawCircle(
                    color = Color.White,
                    radius = (p.radius * 0.5f) * scale,
                    center = Offset(pX, pY)
                )
            }

            // 6. Draw Zombies
            for (z in sim.zombies) {
                val zX = offsetX + z.x * scale
                val zY = offsetY + z.y * scale
                val zR = z.radius * scale

                val baseColor = when (z.type) {
                    ZombieType.WALKER -> ToxicEmerald
                    ZombieType.RUNNER -> DangerRed
                    ZombieType.TANK -> Color(0xFF795548)
                    ZombieType.SPITTER -> Color(0xFFCDDC39)
                    ZombieType.BOSS -> Color(0xFF9C27B0)
                }

                // Zombie body
                drawCircle(color = baseColor, radius = zR, center = Offset(zX, zY))
                drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = zR, center = Offset(zX, zY), style = Stroke(width = 2f))

                // Zombie Glowing Eyes
                val eyeOffX = zR * 0.35f
                val eyeOffY = zR * 0.25f
                drawCircle(color = Color(0xFFFF1744), radius = 2.2f * scale, center = Offset(zX - eyeOffX, zY - eyeOffY))
                drawCircle(color = Color(0xFFFF1744), radius = 2.2f * scale, center = Offset(zX + eyeOffX, zY - eyeOffY))

                // Zombie Health Bar
                if (z.health < z.maxHealth) {
                    val bw = zR * 2.2f
                    val bh = 3.5f * scale
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(zX - bw / 2, zY - zR - 8f * scale),
                        size = Size(bw, bh)
                    )
                    drawRect(
                        color = ToxicEmerald,
                        topLeft = Offset(zX - bw / 2, zY - zR - 8f * scale),
                        size = Size(bw * (z.health / z.maxHealth), bh)
                    )
                }
            }

            // 7. Draw Players
            for (p in sim.players) {
                if (p.health <= 0) continue
                val pX = offsetX + p.x * scale
                val pY = offsetY + p.y * scale
                val pR = 15f * scale
                val pColor = PlayerColors.getOrElse(p.colorIndex) { NeonCyan }

                // Aiming Barrel
                val barrelLen = 22f * scale
                drawLine(
                    color = pColor,
                    start = Offset(pX, pY),
                    end = Offset(pX + cos(p.angle) * barrelLen, pY + sin(p.angle) * barrelLen),
                    strokeWidth = 4f * scale
                )

                // Player Body
                drawCircle(color = pColor, radius = pR, center = Offset(pX, pY))
                drawCircle(color = Color.White, radius = pR, center = Offset(pX, pY), style = Stroke(width = 2.dp.toPx()))

                // Player HP Bar
                val barW = 32f * scale
                val barH = 3.5f * scale
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(pX - barW / 2, pY - pR - 10f * scale),
                    size = Size(barW, barH)
                )
                drawRect(
                    color = if (p.health > 35) ToxicEmerald else DangerRed,
                    topLeft = Offset(pX - barW / 2, pY - pR - 10f * scale),
                    size = Size(barW * (p.health / 100f), barH)
                )

                // Callsign Label
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f * scale
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    drawText(p.name, pX, pY - pR - 14f * scale, paint)
                }
            }

            // 8. Draw Particle Bursts
            for (pt in sim.particles) {
                val ptX = offsetX + pt.x * scale
                val ptY = offsetY + pt.y * scale
                drawCircle(
                    color = Color(pt.color).copy(alpha = pt.alpha),
                    radius = pt.size * scale,
                    center = Offset(ptX, ptY)
                )
            }

            // 9. Draw Floating Damage / Upgrade Texts
            for (ft in sim.floatingTexts) {
                val ftX = offsetX + ft.x * scale
                val ftY = offsetY + ft.y * scale
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (ft.alpha * 255).toInt(),
                            ((ft.color shr 16) and 0xFF).toInt(),
                            ((ft.color shr 8) and 0xFF).toInt(),
                            (ft.color and 0xFF).toInt()
                        )
                        textSize = 26f * scale
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText(ft.text, ftX, ftY, paint)
                }
            }
        }

        // Top HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface.copy(alpha = 0.9f))
                        .border(1.dp, CrystalViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WAVE ${sim.wave.waveNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        color = CrystalViolet,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (sim.wave.status) {
                            WaveStatus.COUNTDOWN -> "Starts in ${sim.wave.countdownRemaining.toInt()}s"
                            WaveStatus.ACTIVE -> "${sim.wave.zombiesRemainingToKill} Remaining"
                            WaveStatus.CLEARED -> "Cleared!"
                            WaveStatus.GAME_OVER -> "Destroyed"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sim.wave.status == WaveStatus.COUNTDOWN) ToxicEmerald else NeonCyan
                    )
                }

                // Crystal Integrity Bar
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface.copy(alpha = 0.9f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CORE: ${sim.crystal.health.toInt()} / ${sim.crystal.maxHealth.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (sim.crystal.health / sim.crystal.maxHealth).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(110.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (sim.crystal.health > 250) NeonCyan else DangerRed,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }

                // Shards & Exit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface.copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦ ${localPlayer?.shards ?: 0}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD700)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurface.copy(alpha = 0.8f))
                            .testTag("exit_game_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Leave Match",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Upgrades Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val shards = localPlayer?.shards ?: 0

                // Repair Button
                UpgradeButton(
                    title = "Repair",
                    cost = UpgradeType.REPAIR_CRYSTAL.cost,
                    icon = Icons.Default.Healing,
                    enabled = shards >= UpgradeType.REPAIR_CRYSTAL.cost,
                    accent = NeonCyan,
                    testTag = "upgrade_repair_btn"
                ) {
                    viewModel.onLocalPlayerUpgrade(UpgradeType.REPAIR_CRYSTAL)
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Damage Button
                UpgradeButton(
                    title = "Dmg +",
                    cost = UpgradeType.WEAPON_DAMAGE.cost,
                    icon = Icons.Default.ElectricBolt,
                    enabled = shards >= UpgradeType.WEAPON_DAMAGE.cost,
                    accent = Color(0xFFFF4081),
                    testTag = "upgrade_dmg_btn"
                ) {
                    viewModel.onLocalPlayerUpgrade(UpgradeType.WEAPON_DAMAGE)
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Speed Button
                UpgradeButton(
                    title = "Speed +",
                    cost = UpgradeType.MOVE_SPEED.cost,
                    icon = Icons.Default.Speed,
                    enabled = shards >= UpgradeType.MOVE_SPEED.cost,
                    accent = ToxicEmerald,
                    testTag = "upgrade_speed_btn"
                ) {
                    viewModel.onLocalPlayerUpgrade(UpgradeType.MOVE_SPEED)
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Shockwave Blast Button
                UpgradeButton(
                    title = "Shockwave",
                    cost = UpgradeType.CRYSTAL_SHOCKWAVE.cost,
                    icon = Icons.Default.FlashOn,
                    enabled = shards >= UpgradeType.CRYSTAL_SHOCKWAVE.cost,
                    accent = Color(0xFFFFD700),
                    testTag = "upgrade_shockwave_btn"
                ) {
                    viewModel.onLocalPlayerUpgrade(UpgradeType.CRYSTAL_SHOCKWAVE)
                }
            }
        }

        // Bottom Twin-Stick On-Screen Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Stick: Movement
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VirtualJoystick(
                    sizeDp = 136.dp,
                    accentColor = NeonCyan,
                    isAimStick = false,
                    testTag = "move_joystick"
                ) { dx, dy, _, _ ->
                    viewModel.onLocalPlayerInput(
                        dx = dx,
                        dy = dy,
                        aimAngle = localPlayer?.aimAngle ?: 0f,
                        isFiring = localPlayer?.isFiring ?: false
                    )
                }
                Text(
                    text = "MOVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Right Stick: Aim & Rapid Fire
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VirtualJoystick(
                    sizeDp = 136.dp,
                    accentColor = Color(0xFFFF4081),
                    isAimStick = true,
                    testTag = "aim_joystick"
                ) { _, _, angle, isEngaged ->
                    viewModel.onLocalPlayerInput(
                        dx = localPlayer?.moveDx ?: 0f,
                        dy = localPlayer?.moveDy ?: 0f,
                        aimAngle = angle,
                        isFiring = isEngaged
                    )
                }
                Text(
                    text = "AIM & FIRE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4081).copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Game Over Overlay
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE60A0E17))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .testTag("game_over_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 2.dp, brush = Brush.linearGradient(listOf(DangerRed, CrystalViolet)))
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CRYSTAL DESTROYED!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DangerRed,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "The ancient core has fallen to the zombie swarm.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                        )

                        // Stats Summary Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0A0E17))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WAVES", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${lastRecord?.waveReached ?: sim.wave.waveNumber}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrystalViolet
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ZOMBIES", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${lastRecord?.zombiesKilled ?: sim.totalZombiesKilled}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ToxicEmerald
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SURVIVED", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${lastRecord?.survivalSeconds ?: 0}s",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = { viewModel.restartMatch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("retry_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color(0xFF0A0E17)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DEFEND AGAIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = { viewModel.leaveGame() },
                            modifier = Modifier.testTag("menu_btn")
                        ) {
                            Text("Return to Menu", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Leave Confirmation Dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Leave Match?", color = TextPrimary) },
                text = { Text("Current wave progress will end and return to the main menu.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            viewModel.leaveGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Leave Match")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Continue Playing", color = NeonCyan)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
private fun UpgradeButton(
    title: String,
    cost: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) DarkSurface else DarkSurface.copy(alpha = 0.4f))
            .border(
                1.dp,
                if (enabled) accent.copy(alpha = 0.6f) else DarkSurfaceBorder.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) accent else TextMuted,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) TextPrimary else TextMuted
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "${cost}✦",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color(0xFFFFD700) else TextMuted
        )
    }
}
