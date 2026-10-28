package com.example.model

enum class ZombieType(
    val displayName: String,
    val baseHealth: Float,
    val baseSpeed: Float,
    val damage: Float,
    val radius: Float,
    val scoreReward: Int,
    val shardReward: Int,
    val colorHex: Long
) {
    WALKER("Walker", 35f, 75f, 12f, 16f, 10, 1, 0xFF4CAF50),
    RUNNER("Runner", 20f, 150f, 8f, 13f, 15, 1, 0xFFFF5722),
    TANK("Hulk Tank", 160f, 45f, 35f, 26f, 40, 4, 0xFF795548),
    SPITTER("Acid Spitter", 45f, 65f, 15f, 18f, 25, 2, 0xFFCDDC39),
    BOSS("Mutant Abomination", 650f, 60f, 50f, 38f, 150, 12, 0xFF9C27B0)
}

enum class WaveStatus {
    COUNTDOWN,
    ACTIVE,
    CLEARED,
    GAME_OVER
}

data class Player(
    val id: String,
    var name: String,
    val colorIndex: Int,
    var x: Float,
    var y: Float,
    var angle: Float = 0f,
    var health: Int = 100,
    val maxHealth: Int = 100,
    var shards: Int = 0,
    var score: Int = 0,
    var weaponDamageLevel: Int = 1,
    var fireRateLevel: Int = 1,
    var speedLevel: Int = 1,
    val isHost: Boolean = false,
    val deviceType: String = "Android",
    var lastShootTime: Long = 0L,
    var moveDx: Float = 0f,
    var moveDy: Float = 0f,
    var isFiring: Boolean = false,
    var aimAngle: Float = 0f
)

data class Zombie(
    val id: Long,
    val type: ZombieType,
    var x: Float,
    var y: Float,
    var health: Float,
    val maxHealth: Float,
    val speed: Float,
    val damage: Float,
    val radius: Float,
    val scoreReward: Int,
    val shardReward: Int,
    var attackCooldown: Float = 0f,
    var spitCooldown: Float = 0f
)

data class Projectile(
    val id: Long,
    val ownerId: String,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val colorIndex: Int,
    val isEnemy: Boolean = false,
    var lifetime: Float = 1.6f,
    val radius: Float = 4.5f
)

data class CrystalShard(
    val id: Long,
    var x: Float,
    var y: Float,
    val value: Int,
    var lifetime: Float = 18f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Long,
    var alpha: Float = 1f,
    val size: Float = 4f,
    var maxLife: Float = 0.4f,
    var currentLife: Float = 0.4f
)

data class FloatingText(
    val id: Long,
    val text: String,
    var x: Float,
    var y: Float,
    val color: Long,
    var alpha: Float = 1f,
    var lifetime: Float = 0.8f
)

data class CrystalCore(
    var health: Float = 1000f,
    val maxHealth: Float = 1000f,
    var shield: Float = 250f,
    val maxShield: Float = 250f,
    val x: Float = 600f,
    val y: Float = 600f,
    val radius: Float = 44f,
    var pulseCooldown: Float = 0f,
    var lastHitFlash: Float = 0f
)

data class WaveState(
    var waveNumber: Int = 1,
    var status: WaveStatus = WaveStatus.COUNTDOWN,
    var countdownRemaining: Float = 4f,
    var totalZombiesInWave: Int = 10,
    var zombiesSpawned: Int = 0,
    var zombiesRemainingToKill: Int = 10,
    var spawnTimer: Float = 0f
)

enum class UpgradeType(
    val title: String,
    val cost: Int,
    val description: String
) {
    WEAPON_DAMAGE("Plasma Power", 5, "+25% projectile damage"),
    FIRE_RATE("Rapid Capacitor", 5, "+20% firing speed"),
    MOVE_SPEED("Thrusters", 4, "+15% movement agility"),
    REPAIR_CRYSTAL("Crystal Mending", 8, "Restores +250 Crystal HP"),
    CRYSTAL_SHOCKWAVE("Energy Shockwave", 10, "Blasts all nearby zombies")
}
