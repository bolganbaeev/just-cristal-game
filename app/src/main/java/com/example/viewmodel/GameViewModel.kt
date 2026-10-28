package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.ScoreRecord
import com.example.engine.GameSimulation
import com.example.model.UpgradeType
import com.example.server.LanClient
import com.example.server.LanGameServer
import com.example.sound.GameAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class AppScreen {
    HOME,
    HOST_LOBBY,
    JOIN_LOBBY,
    ARENA,
    SCORES
}

enum class MatchMode {
    SOLO,
    LAN_HOST,
    LAN_CLIENT
}

data class ConnectedPeer(
    val id: String,
    val name: String,
    val device: String,
    val colorIndex: Int
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val audio = GameAudio(application)
    private val db = GameDatabase.getInstance(application)
    private val scoreDao = db.scoreDao()

    val topScores = scoreDao.getTopScores()
    val highestWave = scoreDao.getHighestWave()
    val totalZombiesKilled = scoreDao.getTotalZombiesKilled()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _matchMode = MutableStateFlow(MatchMode.SOLO)
    val matchMode: StateFlow<MatchMode> = _matchMode.asStateFlow()

    private val _playerName = MutableStateFlow("Commander Android")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _hostIpAddress = MutableStateFlow("127.0.0.1")
    val hostIpAddress: StateFlow<String> = _hostIpAddress.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<ConnectedPeer>>(emptyList())
    val connectedPeers: StateFlow<List<ConnectedPeer>> = _connectedPeers.asStateFlow()

    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _lastMatchRecord = MutableStateFlow<ScoreRecord?>(null)
    val lastMatchRecord: StateFlow<ScoreRecord?> = _lastMatchRecord.asStateFlow()

    // Host authoritative simulation
    lateinit var simulation: GameSimulation
    private var lanServer: LanGameServer? = null
    private var lanClient: LanClient? = null
    private var gameLoopJob: Job? = null
    private var broadcastJob: Job? = null

    // For client mode remote render
    private val _remoteStateJson = MutableStateFlow<String?>(null)
    val remoteStateJson: StateFlow<String?> = _remoteStateJson.asStateFlow()

    init {
        initSimulation()
    }

    private fun initSimulation() {
        simulation = GameSimulation(
            onSfxTriggered = { sfx ->
                when (sfx) {
                    "SHOOT" -> audio.playShoot()
                    "HIT" -> audio.playZombieHit()
                    "ZOMBIE_DEATH" -> audio.playZombieDeath()
                    "CRYSTAL_HIT" -> audio.playCrystalHit()
                    "SHARD" -> audio.playShardPickup()
                    "EXPLOSION" -> audio.playExplosion()
                    "WAVE_CLEAR" -> audio.playWaveClear()
                }
                lanServer?.broadcastSfx(sfx)
            },
            onGameOver = { waves, zombiesKilled, survivalSec ->
                _isGameOver.value = true
                val record = ScoreRecord(
                    waveReached = waves,
                    zombiesKilled = zombiesKilled,
                    crystalHealthLeft = 0,
                    survivalSeconds = survivalSec,
                    playerCount = simulation.players.size
                )
                _lastMatchRecord.value = record
                viewModelScope.launch(Dispatchers.IO) {
                    scoreDao.insertRecord(record)
                }
            }
        )
    }

    fun setPlayerName(name: String) {
        _playerName.value = name.take(14)
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun startSoloGame() {
        _matchMode.value = MatchMode.SOLO
        _isGameOver.value = false
        stopServer()
        stopClient()

        simulation.resetGame()
        simulation.players.clear()
        simulation.addPlayer(
            id = "host_local",
            name = _playerName.value,
            deviceType = "Android",
            isHost = true
        )

        startGameLoop()
        _isGameActive.value = true
        _currentScreen.value = AppScreen.ARENA
    }

    fun prepareHostLobby() {
        _matchMode.value = MatchMode.LAN_HOST
        _isGameOver.value = false
        stopClient()
        stopServer()

        lanServer = LanGameServer(
            context = getApplication(),
            port = 8080,
            onClientJoined = { clientId, name, device ->
                val p = simulation.addPlayer(clientId, name, device, isHost = false)
                updatePeersList()
            },
            onClientInput = { clientId, dx, dy, aimAngle, isFiring ->
                simulation.updatePlayerInput(clientId, dx, dy, aimAngle, isFiring)
            },
            onClientUpgrade = { clientId, upgradeType ->
                simulation.applyUpgrade(clientId, upgradeType)
            },
            onClientLeft = { clientId ->
                simulation.removePlayer(clientId)
                updatePeersList()
            }
        )
        lanServer?.start()
        _hostIpAddress.value = lanServer?.getLocalIpAddress() ?: "127.0.0.1"

        simulation.resetGame()
        simulation.players.clear()
        simulation.addPlayer(
            id = "host_local",
            name = _playerName.value,
            deviceType = "Android",
            isHost = true
        )
        updatePeersList()

        _currentScreen.value = AppScreen.HOST_LOBBY
    }

    private fun updatePeersList() {
        val list = simulation.players.map {
            ConnectedPeer(
                id = it.id,
                name = it.name,
                device = it.deviceType,
                colorIndex = it.colorIndex
            )
        }
        _connectedPeers.value = list
    }

    fun startHostGameFromLobby() {
        startGameLoop()
        startBroadcastLoop()
        _isGameActive.value = true
        _currentScreen.value = AppScreen.ARENA
    }

    fun joinGameAsClient(hostIp: String) {
        _matchMode.value = MatchMode.LAN_CLIENT
        _isGameOver.value = false
        stopServer()
        stopClient()

        lanClient = LanClient(
            onStateReceived = { stateJson ->
                _remoteStateJson.value = stateJson
            },
            onConnected = {
                _isGameActive.value = true
                _currentScreen.value = AppScreen.ARENA
            },
            onDisconnected = {
                // If disconnected return to join lobby
                _isGameActive.value = false
            },
            onSfxReceived = { sfx ->
                when (sfx) {
                    "SHOOT" -> audio.playShoot()
                    "HIT" -> audio.playZombieHit()
                    "CRYSTAL_HIT" -> audio.playCrystalHit()
                    "SHARD" -> audio.playShardPickup()
                    "EXPLOSION" -> audio.playExplosion()
                    "WAVE_CLEAR" -> audio.playWaveClear()
                }
            }
        )
        lanClient?.connect(hostIp.trim(), 8080, _playerName.value)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.nanoTime()
            while (isActive && simulation.isRunning) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                simulation.update(dt)
                delay(16) // ~60 FPS
            }
        }
    }

    private fun startBroadcastLoop() {
        broadcastJob?.cancel()
        broadcastJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && simulation.isRunning) {
                val json = simulation.serializeStateJson()
                lanServer?.broadcastState(json)
                delay(33) // ~30 FPS network tick
            }
        }
    }

    fun onLocalPlayerInput(dx: Float, dy: Float, aimAngle: Float, isFiring: Boolean) {
        if (_matchMode.value == MatchMode.LAN_CLIENT) {
            lanClient?.sendInput(dx, dy, aimAngle, isFiring)
        } else {
            simulation.updatePlayerInput("host_local", dx, dy, aimAngle, isFiring)
        }
    }

    fun onLocalPlayerUpgrade(upgradeType: UpgradeType) {
        if (_matchMode.value == MatchMode.LAN_CLIENT) {
            lanClient?.sendUpgrade(upgradeType.name)
        } else {
            simulation.applyUpgrade("host_local", upgradeType.name)
        }
    }

    fun restartMatch() {
        _isGameOver.value = false
        simulation.resetGame()
        startGameLoop()
        if (_matchMode.value == MatchMode.LAN_HOST) {
            startBroadcastLoop()
        }
    }

    fun leaveGame() {
        _isGameActive.value = false
        _isGameOver.value = false
        gameLoopJob?.cancel()
        broadcastJob?.cancel()
        stopServer()
        stopClient()
        _currentScreen.value = AppScreen.HOME
    }

    private fun stopServer() {
        lanServer?.stop()
        lanServer = null
    }

    private fun stopClient() {
        lanClient?.disconnect()
        lanClient = null
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
        stopClient()
        gameLoopJob?.cancel()
        broadcastJob?.cancel()
    }
}
