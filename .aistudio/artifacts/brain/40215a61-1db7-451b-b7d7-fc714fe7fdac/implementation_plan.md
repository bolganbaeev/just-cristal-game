# Cristal Zombie: LAN Co-Op Crystal Defense

A cross-platform co-op top-down twin-stick action game where players defend a glowing ancient crystal from relentless waves of zombies over a local Wi-Fi network (LAN), enabling seamless real-time multiplayer between Android, iPhone, and Web browsers without requiring any app installs on secondary devices.

## User Review & Critical Decisions

> [!IMPORTANT]
> Key architectural and gameplay decisions confirmed based on your preferences:

- **Confirmed Mode & Objective**: Co-op Crystal Defense. Players stand guard around a central glowing Energy Crystal. If the crystal's integrity reaches zero, the match is lost. Defeated zombies drop crystal shards that players gather to repair the crystal, build barricades/turrets, and unlock weapon upgrades.
- **Confirmed Cross-Platform LAN Connectivity**: Android acts as the authoritative LAN Host with an embedded HTTP and WebSocket server. The host screen displays the local LAN IP and an interactive QR code. Friends on iPhone (Safari), iPad, Android, or desktop/laptop browsers on the same Wi-Fi simply scan the QR code to open a companion mobile web client with real-time twin-stick touch controls and synchronized gameplay.
- **Confirmed Controls & Camera**: Top-down twin-stick shooter featuring smooth virtual joysticks (left thumb for 360-degree movement, right thumb for directional aim & rapid fire). Supports both single-player solo defense and multi-player LAN co-op.

---

## 1. Overview & Core Concept

- **What It Does**: Provides an immediate, pick-up-and-play arcade survival experience. Players control heroic defenders guarding a mystical crystal in an arena beset by varied zombie strains (Runners, Hulks, Spitters, and Boss Abominations).
- **Target Audience / Persona**: Friends, families, and co-workers in the same room on local Wi-Fi who want instant multiplayer action without app store downloads, logins, or cloud accounts.
- **Key Value**: Universal cross-device compatibility. The Android host runs the game natively in high-performance Jetpack Compose Canvas, while secondary players on iOS/Web connect with ultra-low latency over local WebSockets.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Lobby & Mode Selection**:
   - Launch into a cyberpunk/dark fantasy title screen featuring glowing neon crystal aesthetics.
   - Choose **Solo Play**, **Host LAN Game**, or **Join LAN Game** (by entering a host's local IP or scanning a room QR).
2. **LAN Host Setup Screen**:
   - Host chooses player character class (e.g., Gunner, Engineer, Pyromancer).
   - Generates local Wi-Fi URL (e.g., `http://192.168.1.45:8080`) and generates an SVG/Canvas QR Code.
   - Live lobby displays connected players (with device badges: Android, iPhone/Web) and ping indicators.
   - Host taps **Start Defense**.
3. **Gameplay Screen (Twin-Stick Arena)**:
   - Dynamic top-down battlefield with the pulsing Crystal Core at the center.
   - HUD displaying: Wave counter, Crystal Integrity bar, Player HP & Shield, Crystal Shard currency, and active players' mini status cards.
   - Dual floating/docked translucent virtual joysticks with tactical haptic feedback.
   - Wave countdown breaks allowing players to upgrade weapons, repair the crystal, or deploy defensive barriers.
4. **Post-Game & High Scores**:
   - Match summary detailing waves survived, zombies slayed, MVP teammate, and shard stats.
   - Local persistent leaderboard (High Waves & Survival Times) stored via Room.

### Visual Identity & Theme
- **Aesthetic Direction**: Sci-fi Dark Crystal / Neon Grimdark. Atmospheric deep obsidian `#0D1117` and slate background with high-contrast electric cyan `#00F0FF`, crystal violet `#A855F7`, and hazardous zombie toxic lime `#10B981` & crimson `#EF4444`.
- **Lighting & Atmosphere**: Glowing radial light halos around the central crystal and player flashlights piercing through the dark arena fog.
- **Sound & Haptics**: Procedural audio effects (synthesized audio tone bursts for shooting, hits, crystal pulse, wave alerts) and Android vibrator haptic pulses on damage.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Embedded Lightweight Local HTTP + WebSocket Server**
  - *Chosen Approach*: Implement a pure Kotlin lightweight NIO/socket HTTP server and WebSocket bridge on the Android device hosting port 8080. It serves an optimized, self-contained single-page HTML5/Canvas touch web app to connecting browsers.
  - *Why*: Eliminates the need for external cloud servers, eliminates internet data usage, provides sub-15ms local latency, and lets iPhone users play immediately through Safari without any App Store installation.
  - *Alternatives Considered*: Firebase Firestore multiplayer was considered but rejected because it introduces cloud latency (~150ms), requires an active internet connection, and requires external cloud accounts.
- **Decision 2: Authoritative Host Simulation Engine**
  - *Chosen Approach*: The Android host runs the authoritative 60 FPS physics and collision loop (player movements, projectile vectors, zombie AI pathfinding towards players/crystal, shard drops). Connected iPhone/Web clients stream input packets (joystick angles, trigger states) and receive compact binary/JSON delta game state updates at 30-60 Hz.
  - *Why*: Avoids desyncs, simplifies anti-cheat, and keeps client logic snappy and responsive across varied hardware.
- **Decision 3: Jetpack Compose Game Canvas**
  - *Chosen Approach*: Native Compose `Canvas` with hardware-accelerated draw passes, particle emission pools, and smooth delta-time calculations using `LaunchedEffect` frame clocks.
  - *Why*: Deep integration with Material 3 HUD overlays, clean state hoisting, and instant navigation without external game engine overhead.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CRISTAL ZOMBIE GAME ARCHITECTURE                 │
└────────────────────────────────────────────────────────────────────────┘

     [ Android Host Device (Native Jetpack Compose) ]
     ┌─────────────────────────────────────────────────────────────┐
     │  GameViewModel & Simulation Engine (Authoritative Loop)     │
     │  - Central Crystal Core State (Health, Shield, Radius)      │
     │  - Zombie Horde Spawner & AI Pathfinding                    │
     │  - Particle System & Projectile Physics Engine              │
     │  - Shards, Upgrades & Wave Progression Manager              │
     └───────────────┬─────────────────────────────▲───────────────┘
                     │ Game State Broadcast        │ Player Inputs
                     ▼                             │
     ┌───────────────────────────────┐   ┌─────────────────────────┐
     │ Native Host Game Screen       │   │ Embedded LAN Web Server │
     │ - Jetpack Compose Canvas Arena│   │ - HTTP File Server      │
     │ - Twin-Stick Touch Controls   │   │ - WebSocket Bridge      │
     │ - Material 3 HUD & Stats      │   │ - Host IP & QR Code Gen │
     └───────────────────────────────┘   └────────────┬────────────┘
                                                      │ LAN Wi-Fi
                                                      ▼
                            ┌──────────────────────────────────────────────┐
                            │ iPhone / Web / Android LAN Clients (Safari)  │
                            │ - Self-Contained HTML5 + Canvas Web Client   │
                            │ - Dual Touch Virtual Joysticks               │
                            │ - Real-time WebSocket Input / State Sync     │
                            └──────────────────────────────────────────────┘
```

### Core Entities & State Models
- **GameState**: Wave number, wave status (Preparation vs Assault), Crystal health (0-1000), Crystal max health, game time elapsed, active players list, zombies list, projectiles list, crystal shards list, and particle effects.
- **PlayerEntity**: ID, name, device type (Host Android, LAN iPhone, LAN Web), color/avatar, position `(x, y)`, velocity, angle, health, max health, weapon loadout (Pistol, Shotgun, Plasma Rifle, Crystal Beam), score, and collected shards.
- **ZombieEntity**: ID, type (Normal, Fast Runner, Tank Hulk, Spitter), position `(x, y)`, health, speed, damage, target (nearest player or crystal core), and attack cooldown.
- **LAN Protocol Packets**:
  - `HELLO`: Client handshake with device name.
  - `INPUT`: Joystick move `(dx, dy)` and aim `(angle, isFiring)`.
  - `STATE`: Packed delta updates of active entities.
  - `EVENT`: Sound/FX triggers (explosion, level up, wave clear).

### Local Persistence (Room Database)
- **ScoreRecordEntity**: High wave reached, survival duration, zombies killed, game date, co-op player count.
- **SettingsEntity**: Sound volume, joystick haptic feedback intensity, preferred player name and color.
