package com.example.engine

import com.example.model.CrystalCore
import com.example.model.CrystalShard
import com.example.model.FloatingText
import com.example.model.Particle
import com.example.model.Player
import com.example.model.Projectile
import com.example.model.UpgradeType
import com.example.model.WaveState
import com.example.model.WaveStatus
import com.example.model.Zombie
import com.example.model.ZombieType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameSimulation(
    private val onSfxTriggered: (sfxType: String) -> Unit,
    private val onGameOver: (wavesSurvived: Int, totalZombiesKilled: Int, survivalSeconds: Long) -> Unit
) {
    companion object {
        const val ARENA_WIDTH = 1200f
        const val ARENA_HEIGHT = 1200f
    }

    private val nextId = AtomicLong(1)
    val crystal = CrystalCore()
    val wave = WaveState()
    val players = CopyOnWriteArrayList<Player>()
    val zombies = CopyOnWriteArrayList<Zombie>()
    val projectiles = CopyOnWriteArrayList<Projectile>()
    val shards = CopyOnWriteArrayList<CrystalShard>()
    val particles = CopyOnWriteArrayList<Particle>()
    val floatingTexts = CopyOnWriteArrayList<FloatingText>()

    var totalZombiesKilled = 0
    var gameStartTime = System.currentTimeMillis()
    var isRunning = true

    init {
        resetGame()
    }

    fun resetGame() {
        crystal.health = crystal.maxHealth
        crystal.shield = crystal.maxShield
        crystal.lastHitFlash = 0f
        wave.waveNumber = 1
        wave.status = WaveStatus.COUNTDOWN
        wave.countdownRemaining = 4f
        wave.totalZombiesInWave = 10
        wave.zombiesSpawned = 0
        wave.zombiesRemainingToKill = 10
        wave.spawnTimer = 0f
        zombies.clear()
        projectiles.clear()
        shards.clear()
        particles.clear()
        floatingTexts.clear()
        totalZombiesKilled = 0
        gameStartTime = System.currentTimeMillis()
        isRunning = true

        // Reset player positions around crystal
        val angleStep = (2 * Math.PI / players.size.coerceAtLeast(1)).toFloat()
        players.forEachIndexed { index, p ->
            p.health = 100
            p.shards = 0
            p.score = 0
            p.weaponDamageLevel = 1
            p.fireRateLevel = 1
            p.speedLevel = 1
            p.x = crystal.x + cos(index * angleStep) * 120f
            p.y = crystal.y + sin(index * angleStep) * 120f
        }
    }

    fun addPlayer(id: String, name: String, deviceType: String, isHost: Boolean): Player {
        val existing = players.find { it.id == id }
        if (existing != null) return existing

        val colorIdx = players.size % 4
        val spawnAngle = (players.size * 1.5f)
        val player = Player(
            id = id,
            name = name,
            colorIndex = colorIdx,
            x = crystal.x + cos(spawnAngle) * 110f,
            y = crystal.y + sin(spawnAngle) * 110f,
            isHost = isHost,
            deviceType = deviceType
        )
        players.add(player)
        return player
    }

    fun removePlayer(id: String) {
        players.removeAll { it.id == id }
    }

    fun updatePlayerInput(id: String, dx: Float, dy: Float, aimAngle: Float, isFiring: Boolean) {
        val player = players.find { it.id == id } ?: return
        player.moveDx = dx.coerceIn(-1f, 1f)
        player.moveDy = dy.coerceIn(-1f, 1f)
        player.aimAngle = aimAngle
        player.isFiring = isFiring
        if (isFiring || hypot(dx, dy) > 0.1f) {
            player.angle = aimAngle
        }
    }

    fun applyUpgrade(playerId: String, upgradeType: String): Boolean {
        val player = players.find { it.id == playerId } ?: return false
        val upgrade = try { UpgradeType.valueOf(upgradeType) } catch (_: Exception) { return false }

        if (player.shards < upgrade.cost) return false
        player.shards -= upgrade.cost

        when (upgrade) {
            UpgradeType.WEAPON_DAMAGE -> {
                player.weaponDamageLevel++
                addFloatingText("+DMG LVL ${player.weaponDamageLevel}", player.x, player.y - 30, 0xFF00E5FF)
            }
            UpgradeType.FIRE_RATE -> {
                player.fireRateLevel++
                addFloatingText("+FIRE RATE LVL ${player.fireRateLevel}", player.x, player.y - 30, 0xFFFF4081)
            }
            UpgradeType.MOVE_SPEED -> {
                player.speedLevel++
                addFloatingText("+SPEED LVL ${player.speedLevel}", player.x, player.y - 30, 0xFF00E676)
            }
            UpgradeType.REPAIR_CRYSTAL -> {
                crystal.health = (crystal.health + 250f).coerceAtMost(crystal.maxHealth)
                addFloatingText("+250 REPAIR", crystal.x, crystal.y - 45, 0xFF00E5FF)
                createBurstParticles(crystal.x, crystal.y, 0xFF00E5FF, 18)
            }
            UpgradeType.CRYSTAL_SHOCKWAVE -> {
                triggerShockwave()
            }
        }
        onSfxTriggered("SHARD")
        return true
    }

    fun triggerShockwave() {
        crystal.pulseCooldown = 1.0f
        onSfxTriggered("EXPLOSION")
        createBurstParticles(crystal.x, crystal.y, 0xFF00E5FF, 36)
        addFloatingText("SHOCKWAVE BLAST!", crystal.x, crystal.y - 60, 0xFF00E5FF)

        // Damage & knockback all zombies within 340px
        for (z in zombies) {
            val dist = hypot(z.x - crystal.x, z.y - crystal.y)
            if (dist < 340f) {
                z.health -= 120f
                val pushAngle = atan2(z.y - crystal.y, z.x - crystal.x)
                z.x += cos(pushAngle) * 90f
                z.y += sin(pushAngle) * 90f
                createBurstParticles(z.x, z.y, 0xFFFF5252, 6)
            }
        }
    }

    fun update(dt: Float) {
        if (!isRunning) return

        // 1. Update Wave State
        updateWave(dt)

        // 2. Update Players
        updatePlayers(dt)

        // 3. Update Projectiles
        updateProjectiles(dt)

        // 4. Update Zombies
        updateZombies(dt)

        // 5. Update Shards
        updateShards(dt)

        // 6. Update Particles & Texts
        updateVFX(dt)

        // 7. Check Game Over
        if (crystal.health <= 0) {
            crystal.health = 0f
            if (wave.status != WaveStatus.GAME_OVER) {
                wave.status = WaveStatus.GAME_OVER
                isRunning = false
                val survivalSec = (System.currentTimeMillis() - gameStartTime) / 1000
                onGameOver(wave.waveNumber, totalZombiesKilled, survivalSec)
            }
        }
    }

    private fun updateWave(dt: Float) {
        when (wave.status) {
            WaveStatus.COUNTDOWN -> {
                wave.countdownRemaining -= dt
                if (wave.countdownRemaining <= 0) {
                    wave.status = WaveStatus.ACTIVE
                    wave.zombiesSpawned = 0
                    wave.totalZombiesInWave = 8 + (wave.waveNumber * 4)
                    wave.zombiesRemainingToKill = wave.totalZombiesInWave
                    wave.spawnTimer = 0.5f
                    onSfxTriggered("WAVE_CLEAR")
                }
            }
            WaveStatus.ACTIVE -> {
                if (wave.zombiesSpawned < wave.totalZombiesInWave) {
                    wave.spawnTimer -= dt
                    if (wave.spawnTimer <= 0) {
                        spawnZombie()
                        wave.zombiesSpawned++
                        // Spawn rate accelerates with wave
                        wave.spawnTimer = (1.8f - (wave.waveNumber * 0.08f)).coerceAtLeast(0.45f)
                    }
                } else if (wave.zombiesRemainingToKill <= 0) {
                    // Wave Cleared!
                    wave.status = WaveStatus.CLEARED
                    onSfxTriggered("WAVE_CLEAR")
                    crystal.health = (crystal.health + 80f).coerceAtMost(crystal.maxHealth)
                    addFloatingText("WAVE ${wave.waveNumber} CLEARED!", crystal.x, crystal.y - 70, 0xFF00E676)
                    wave.waveNumber++
                    wave.countdownRemaining = 4.5f
                    wave.status = WaveStatus.COUNTDOWN
                }
            }
            WaveStatus.CLEARED, WaveStatus.GAME_OVER -> {}
        }
    }

    private fun spawnZombie() {
        val edge = Random.nextInt(4)
        var x = 0f
        var y = 0f
        when (edge) {
            0 -> { x = Random.nextFloat() * ARENA_WIDTH; y = 10f } // Top
            1 -> { x = ARENA_WIDTH - 10f; y = Random.nextFloat() * ARENA_HEIGHT } // Right
            2 -> { x = Random.nextFloat() * ARENA_WIDTH; y = ARENA_HEIGHT - 10f } // Bottom
            3 -> { x = 10f; y = Random.nextFloat() * ARENA_HEIGHT } // Left
        }

        val type = when {
            wave.waveNumber >= 5 && wave.zombiesSpawned == 0 && wave.waveNumber % 5 == 0 -> ZombieType.BOSS
            wave.waveNumber >= 3 && Random.nextFloat() < 0.22f -> ZombieType.TANK
            wave.waveNumber >= 2 && Random.nextFloat() < 0.25f -> ZombieType.RUNNER
            wave.waveNumber >= 3 && Random.nextFloat() < 0.20f -> ZombieType.SPITTER
            else -> ZombieType.WALKER
        }

        val hpMultiplier = 1f + (wave.waveNumber - 1) * 0.12f
        val z = Zombie(
            id = nextId.getAndIncrement(),
            type = type,
            x = x,
            y = y,
            health = type.baseHealth * hpMultiplier,
            maxHealth = type.baseHealth * hpMultiplier,
            speed = type.baseSpeed,
            damage = type.damage,
            radius = type.radius,
            scoreReward = type.scoreReward,
            shardReward = type.shardReward
        )
        zombies.add(z)
    }

    private fun updatePlayers(dt: Float) {
        val now = System.currentTimeMillis()

        for (p in players) {
            if (p.health <= 0) continue

            // Movement
            val speed = 200f + (p.speedLevel * 25f)
            p.x = (p.x + p.moveDx * speed * dt).coerceIn(30f, ARENA_WIDTH - 30f)
            p.y = (p.y + p.moveDy * speed * dt).coerceIn(30f, ARENA_HEIGHT - 30f)

            // Prevent overlapping inside the crystal core
            val distToCrystal = hypot(p.x - crystal.x, p.y - crystal.y)
            if (distToCrystal < crystal.radius + 14f) {
                val angle = atan2(p.y - crystal.y, p.x - crystal.x)
                p.x = crystal.x + cos(angle) * (crystal.radius + 15f)
                p.y = crystal.y + sin(angle) * (crystal.radius + 15f)
            }

            // Shooting
            if (p.isFiring) {
                val shootInterval = (280L - (p.fireRateLevel * 30L)).coerceAtLeast(90L)
                if (now - p.lastShootTime >= shootInterval) {
                    p.lastShootTime = now
                    val barrelDist = 22f
                    val spawnX = p.x + cos(p.aimAngle) * barrelDist
                    val spawnY = p.y + sin(p.aimAngle) * barrelDist
                    val projSpeed = 580f

                    val proj = Projectile(
                        id = nextId.getAndIncrement(),
                        ownerId = p.id,
                        x = spawnX,
                        y = spawnY,
                        vx = cos(p.aimAngle) * projSpeed,
                        vy = sin(p.aimAngle) * projSpeed,
                        damage = 22f + (p.weaponDamageLevel * 8f),
                        colorIndex = p.colorIndex
                    )
                    projectiles.add(proj)
                    onSfxTriggered("SHOOT")
                }
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val iter = projectiles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.lifetime -= dt

            if (p.lifetime <= 0 || p.x !in 0f..ARENA_WIDTH || p.y !in 0f..ARENA_HEIGHT) {
                projectiles.remove(p)
                continue
            }

            if (!p.isEnemy) {
                // Check collision with zombies
                var hit = false
                for (z in zombies) {
                    val dist = hypot(z.x - p.x, z.y - p.y)
                    if (dist < z.radius + p.radius) {
                        z.health -= p.damage
                        hit = true
                        createBurstParticles(p.x, p.y, 0xFF00E5FF, 4)
                        onSfxTriggered("HIT")

                        if (z.health <= 0) {
                            // Zombie died
                            totalZombiesKilled++
                            wave.zombiesRemainingToKill = (wave.zombiesRemainingToKill - 1).coerceAtLeast(0)
                            zombies.remove(z)
                            createBurstParticles(z.x, z.y, z.type.colorHex, 10)
                            onSfxTriggered("ZOMBIE_DEATH")

                            // Spawn crystal shards
                            val shardCount = z.shardReward
                            for (s in 0 until shardCount) {
                                shards.add(
                                    CrystalShard(
                                        id = nextId.getAndIncrement(),
                                        x = z.x + Random.nextFloat() * 20f - 10f,
                                        y = z.y + Random.nextFloat() * 20f - 10f,
                                        value = 1
                                    )
                                )
                            }

                            // Award score
                            players.find { it.id == p.ownerId }?.let { shooter ->
                                shooter.score += z.scoreReward
                            }
                        }
                        break
                    }
                }
                if (hit) {
                    projectiles.remove(p)
                }
            } else {
                // Enemy acid projectile hitting player or crystal
                val distToCrystal = hypot(p.x - crystal.x, p.y - crystal.y)
                if (distToCrystal < crystal.radius) {
                    damageCrystal(p.damage)
                    projectiles.remove(p)
                    createBurstParticles(p.x, p.y, 0xFFFF5252, 6)
                    continue
                }
                for (player in players) {
                    if (player.health > 0 && hypot(p.x - player.x, p.y - player.y) < 20f) {
                        player.health = (player.health - p.damage.toInt()).coerceAtLeast(0)
                        projectiles.remove(p)
                        createBurstParticles(p.x, p.y, 0xFFFF5252, 6)
                        break
                    }
                }
            }
        }
    }

    private fun updateZombies(dt: Float) {
        for (z in zombies) {
            z.attackCooldown -= dt
            z.spitCooldown -= dt

            // Target evaluation: closest living player vs central crystal
            var targetX = crystal.x
            var targetY = crystal.y
            var minPlayerDist = Float.MAX_VALUE
            var targetPlayer: Player? = null

            for (p in players) {
                if (p.health > 0) {
                    val d = hypot(p.x - z.x, p.y - z.y)
                    if (d < minPlayerDist) {
                        minPlayerDist = d
                        targetPlayer = p
                    }
                }
            }

            // Walkers/Tanks target crystal primarily if within 300px, otherwise nearest player
            val distToCrystal = hypot(crystal.x - z.x, crystal.y - z.y)
            if (targetPlayer != null && (minPlayerDist < 180f || z.type == ZombieType.RUNNER)) {
                targetX = targetPlayer.x
                targetY = targetPlayer.y
            }

            val angle = atan2(targetY - z.y, targetX - z.x)

            // Spitter behavior: stops at distance 240 to launch acid
            if (z.type == ZombieType.SPITTER && hypot(targetX - z.x, targetY - z.y) < 250f) {
                if (z.spitCooldown <= 0) {
                    z.spitCooldown = 2.4f
                    val acid = Projectile(
                        id = nextId.getAndIncrement(),
                        ownerId = "zombie",
                        x = z.x,
                        y = z.y,
                        vx = cos(angle) * 240f,
                        vy = sin(angle) * 240f,
                        damage = z.damage,
                        colorIndex = 1,
                        isEnemy = true,
                        lifetime = 2.5f
                    )
                    projectiles.add(acid)
                }
            } else {
                // Move towards target
                z.x += cos(angle) * z.speed * dt
                z.y += sin(angle) * z.speed * dt
            }

            // Attack Crystal Core
            if (distToCrystal < crystal.radius + z.radius + 5f) {
                if (z.attackCooldown <= 0) {
                    z.attackCooldown = 0.9f
                    damageCrystal(z.damage)
                }
            }

            // Attack Players
            for (p in players) {
                if (p.health > 0 && hypot(p.x - z.x, p.y - z.y) < z.radius + 16f) {
                    if (z.attackCooldown <= 0) {
                        z.attackCooldown = 0.8f
                        p.health = (p.health - z.damage.toInt()).coerceAtLeast(0)
                        createBurstParticles(p.x, p.y, 0xFFFF5252, 6)
                        onSfxTriggered("HIT")
                    }
                }
            }
        }
    }

    private fun damageCrystal(amount: Float) {
        crystal.lastHitFlash = 0.25f
        if (crystal.shield > 0) {
            val shieldDmg = amount.coerceAtMost(crystal.shield)
            crystal.shield -= shieldDmg
            val remaining = amount - shieldDmg
            if (remaining > 0) crystal.health -= remaining
        } else {
            crystal.health -= amount
        }
        createBurstParticles(crystal.x, crystal.y, 0xFFFF5252, 5)
        onSfxTriggered("CRYSTAL_HIT")
    }

    private fun updateShards(dt: Float) {
        val iter = shards.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.lifetime -= dt
            if (s.lifetime <= 0) {
                shards.remove(s)
                continue
            }

            // Magnetic attraction to nearby living players
            for (p in players) {
                if (p.health > 0) {
                    val dist = hypot(p.x - s.x, p.y - s.y)
                    if (dist < 80f) {
                        val angle = atan2(p.y - s.y, p.x - s.x)
                        s.x += cos(angle) * 320f * dt
                        s.y += sin(angle) * 320f * dt
                    }
                    if (dist < 26f) {
                        p.shards += s.value
                        p.score += s.value * 5
                        shards.remove(s)
                        createBurstParticles(s.x, s.y, 0xFFFFD700, 3)
                        onSfxTriggered("SHARD")
                        break
                    }
                }
            }
        }
    }

    private fun updateVFX(dt: Float) {
        if (crystal.lastHitFlash > 0) {
            crystal.lastHitFlash -= dt
        }

        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.currentLife -= dt
            p.alpha = (p.currentLife / p.maxLife).coerceIn(0f, 1f)
            if (p.currentLife <= 0) {
                particles.remove(p)
            }
        }

        val tIter = floatingTexts.iterator()
        while (tIter.hasNext()) {
            val t = tIter.next()
            t.y -= 25f * dt
            t.lifetime -= dt
            t.alpha = (t.lifetime / 0.8f).coerceIn(0f, 1f)
            if (t.lifetime <= 0) {
                floatingTexts.remove(t)
            }
        }
    }

    private fun createBurstParticles(x: Float, y: Float, colorHex: Long, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 120f + 30f
            val life = Random.nextFloat() * 0.3f + 0.2f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = colorHex,
                    maxLife = life,
                    currentLife = life,
                    size = Random.nextFloat() * 4f + 2f
                )
            )
        }
    }

    fun addFloatingText(text: String, x: Float, y: Float, colorHex: Long) {
        floatingTexts.add(
            FloatingText(
                id = nextId.getAndIncrement(),
                text = text,
                x = x,
                y = y,
                color = colorHex
            )
        )
    }

    fun serializeStateJson(): String {
        val root = JSONObject()
        root.put("type", "STATE")

        val state = JSONObject()

        // Crystal
        val cObj = JSONObject().apply {
            put("health", crystal.health.toInt())
            put("maxHealth", crystal.maxHealth.toInt())
            put("shield", crystal.shield.toInt())
            put("x", crystal.x.toInt())
            put("y", crystal.y.toInt())
            put("radius", crystal.radius.toInt())
        }
        state.put("crystal", cObj)

        // Wave
        val wObj = JSONObject().apply {
            put("waveNumber", wave.waveNumber)
            put("status", wave.status.name)
            put("remaining", wave.zombiesRemainingToKill)
            put("countdown", wave.countdownRemaining.toInt())
        }
        state.put("wave", wObj)

        // Players
        val pArr = JSONArray()
        for (p in players) {
            val po = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("colorIndex", p.colorIndex)
                put("x", p.x.toInt())
                put("y", p.y.toInt())
                put("angle", p.angle.toDouble())
                put("health", p.health)
                put("shards", p.shards)
                put("score", p.score)
                put("deviceType", p.deviceType)
            }
            pArr.put(po)
        }
        state.put("players", pArr)

        // Zombies
        val zArr = JSONArray()
        for (z in zombies) {
            val zo = JSONObject().apply {
                put("id", z.id)
                put("type", z.type.name)
                put("x", z.x.toInt())
                put("y", z.y.toInt())
                put("health", z.health.toInt())
                put("maxHealth", z.maxHealth.toInt())
                put("radius", z.radius.toInt())
            }
            zArr.put(zo)
        }
        state.put("zombies", zArr)

        // Projectiles
        val prArr = JSONArray()
        for (pr in projectiles) {
            val pro = JSONObject().apply {
                put("x", pr.x.toInt())
                put("y", pr.y.toInt())
                put("isEnemy", pr.isEnemy)
                put("radius", pr.radius.toInt())
            }
            prArr.put(pro)
        }
        state.put("projectiles", prArr)

        // Shards
        val shArr = JSONArray()
        for (s in shards) {
            val sho = JSONObject().apply {
                put("x", s.x.toInt())
                put("y", s.y.toInt())
            }
            shArr.put(sho)
        }
        state.put("shards", shArr)

        root.put("state", state)
        return root.toString()
    }
}
