package com.example.server

object WebClientHtml {
    fun getHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
<title>Cristal Zombie - LAN Companion</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; -webkit-touch-callout: none; -webkit-user-select: none; user-select: none; }
  html, body { width: 100%; height: 100%; overflow: hidden; background: #0a0e17; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; color: #f1f5f9; touch-action: none; }
  #canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; }
  
  /* Top HUD */
  #hud-top {
    position: absolute; top: env(safe-area-inset-top, 8px); left: 0; right: 0;
    display: flex; justify-content: space-between; align-items: center;
    padding: 8px 16px; pointer-events: none; z-index: 10;
  }
  .hud-badge {
    background: rgba(19, 26, 41, 0.85); border: 1px solid rgba(0, 229, 255, 0.3);
    border-radius: 12px; padding: 6px 12px; font-weight: 700; font-size: 13px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.4); backdrop-filter: blur(8px);
  }
  .crystal-bar-wrap {
    display: flex; flex-direction: column; align-items: center; min-width: 140px;
  }
  .crystal-bar-bg {
    width: 100%; height: 8px; background: rgba(255,255,255,0.15); border-radius: 4px; overflow: hidden; margin-top: 3px;
  }
  .crystal-bar-fill {
    height: 100%; width: 100%; background: linear-gradient(90deg, #00e5ff, #b388ff); transition: width 0.15s ease-out;
  }

  /* Upgrade Drawer Button */
  #upgrade-bar {
    position: absolute; top: calc(env(safe-area-inset-top, 8px) + 54px); left: 50%; transform: translateX(-50%);
    display: flex; gap: 8px; z-index: 15;
  }
  .upgrade-btn {
    background: rgba(19, 26, 41, 0.9); border: 1px solid #b388ff; color: #f1f5f9;
    padding: 6px 12px; border-radius: 16px; font-size: 11px; font-weight: 600; cursor: pointer;
    box-shadow: 0 2px 8px rgba(179,136,255,0.3); display: flex; align-items: center; gap: 4px;
  }
  .upgrade-btn:active { background: #b388ff; color: #131a29; }
  .upgrade-btn:disabled { opacity: 0.35; pointer-events: none; }

  /* Joysticks Overlay */
  #joysticks-container {
    position: absolute; bottom: 0; left: 0; right: 0; height: 50%; pointer-events: auto; z-index: 20;
    display: flex; justify-content: space-between;
  }
  .touch-zone {
    width: 50%; height: 100%; position: relative;
  }
  .joystick-base {
    position: absolute; width: 120px; height: 120px; border-radius: 60px;
    background: rgba(255, 255, 255, 0.08); border: 2px solid rgba(0, 229, 255, 0.4);
    transform: translate(-50%, -50%); display: none; pointer-events: none;
    box-shadow: 0 0 20px rgba(0, 229, 255, 0.15);
  }
  .joystick-thumb {
    position: absolute; width: 50px; height: 50px; border-radius: 25px;
    background: radial-gradient(circle, #00e5ff 0%, #00838f 100%);
    box-shadow: 0 0 14px rgba(0, 229, 255, 0.6);
    transform: translate(-50%, -50%); display: none; pointer-events: none;
  }
  .aim-base { border-color: rgba(255, 64, 129, 0.4); box-shadow: 0 0 20px rgba(255, 64, 129, 0.15); }
  .aim-thumb { background: radial-gradient(circle, #ff4081 0%, #c2185b 100%); box-shadow: 0 0 14px rgba(255, 64, 129, 0.6); }

  /* Join Modal */
  #join-overlay {
    position: absolute; inset: 0; background: rgba(10, 14, 23, 0.95);
    display: flex; flex-direction: column; justify-content: center; align-items: center;
    padding: 24px; z-index: 50; text-align: center;
  }
  .join-card {
    background: #131a29; border: 1px solid #2a3b5c; border-radius: 20px;
    padding: 28px 24px; width: 100%; max-width: 360px; box-shadow: 0 8px 32px rgba(0,0,0,0.6);
  }
  .join-card h1 { color: #00e5ff; font-size: 24px; margin-bottom: 6px; letter-spacing: 1px; }
  .join-card p { color: #94a3b8; font-size: 13px; margin-bottom: 20px; line-height: 1.4; }
  .join-input {
    width: 100%; background: #0a0e17; border: 1px solid #2a3b5c; border-radius: 12px;
    padding: 12px 16px; color: #f1f5f9; font-size: 16px; outline: none; margin-bottom: 16px;
  }
  .join-input:focus { border-color: #00e5ff; }
  .join-btn {
    width: 100%; background: linear-gradient(135deg, #00e5ff, #b388ff); border: none;
    border-radius: 12px; padding: 14px; font-weight: 700; font-size: 16px; color: #0a0e17;
    cursor: pointer; box-shadow: 0 4px 16px rgba(0,229,255,0.4);
  }
  .join-btn:active { transform: scale(0.98); }
</style>
</head>
<body>

<canvas id="canvas"></canvas>

<!-- Top HUD -->
<div id="hud-top">
  <div class="hud-badge" id="player-badge">
    <span style="color:#00e5ff">●</span> <span id="player-name-txt">Player</span>
    <span style="color:#ffd700; margin-left:6px;">✦ <span id="shard-txt">0</span></span>
  </div>
  <div class="hud-badge crystal-bar-wrap">
    <div style="font-size:11px; color:#00e5ff;">CRISTAL CORE</div>
    <div class="crystal-bar-bg"><div class="crystal-bar-fill" id="crystal-bar"></div></div>
  </div>
  <div class="hud-badge" id="wave-badge">
    <span style="color:#b388ff;">WAVE</span> <span id="wave-txt">1</span>
  </div>
</div>

<!-- Upgrade Drawer -->
<div id="upgrade-bar">
  <button class="upgrade-btn" id="btn-up-repair" onclick="sendUpgrade('REPAIR_CRYSTAL')">
    💎 Repair (+250) <span style="color:#ffd700;">8✦</span>
  </button>
  <button class="upgrade-btn" id="btn-up-dmg" onclick="sendUpgrade('WEAPON_DAMAGE')">
    ⚡ Dmg <span style="color:#ffd700;">5✦</span>
  </button>
  <button class="upgrade-btn" id="btn-up-blast" onclick="sendUpgrade('CRYSTAL_SHOCKWAVE')">
    💥 Blast <span style="color:#ffd700;">10✦</span>
  </button>
</div>

<!-- Virtual Dual Joysticks -->
<div id="joysticks-container">
  <div class="touch-zone" id="zone-move">
    <div class="joystick-base" id="move-base"></div>
    <div class="joystick-thumb" id="move-thumb"></div>
  </div>
  <div class="touch-zone" id="zone-aim">
    <div class="joystick-base aim-base" id="aim-base"></div>
    <div class="joystick-thumb aim-thumb" id="aim-thumb"></div>
  </div>
</div>

<!-- Join Overlay -->
<div id="join-overlay">
  <div class="join-card">
    <h1>CRISTAL ZOMBIE</h1>
    <p>Connected to LAN Host!<br>Choose your defender handle to enter the crystal defense arena.</p>
    <input type="text" id="name-input" class="join-input" maxlength="12" placeholder="Your Defender Name" value="iOS Survivor">
    <button class="join-btn" onclick="startJoin()">JOIN CO-OP MATCH</button>
  </div>
</div>

<script>
  const canvas = document.getElementById('canvas');
  const ctx = canvas.getContext('2d');
  
  let ws = null;
  let myId = null;
  let myName = "iOS Survivor";
  let gameState = null;
  let audioCtx = null;

  function initAudio() {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
  }

  function playTone(freq1, freq2, duration, type='sine') {
    if (!audioCtx) return;
    try {
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = type;
      osc.frequency.setValueAtTime(freq1, audioCtx.currentTime);
      if (freq2) osc.frequency.exponentialRampToValueAtTime(freq2, audioCtx.currentTime + duration);
      gain.gain.setValueAtTime(0.2, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + duration);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + duration);
    } catch(e) {}
  }

  function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }
  window.addEventListener('resize', resize);
  resize();

  // Joystick state
  let moveTouchId = null;
  let moveStart = { x: 0, y: 0 };
  let moveVector = { dx: 0, dy: 0 };

  let aimTouchId = null;
  let aimStart = { x: 0, y: 0 };
  let aimAngle = 0;
  let isFiring = false;

  const moveZone = document.getElementById('zone-move');
  const moveBase = document.getElementById('move-base');
  const moveThumb = document.getElementById('move-thumb');

  const aimZone = document.getElementById('zone-aim');
  const aimBase = document.getElementById('aim-base');
  const aimThumb = document.getElementById('aim-thumb');

  const MAX_RADIUS = 50;

  // Touch Move
  moveZone.addEventListener('touchstart', (e) => {
    e.preventDefault();
    initAudio();
    for (let t of e.changedTouches) {
      if (moveTouchId === null) {
        moveTouchId = t.identifier;
        moveStart = { x: t.clientX, y: t.clientY };
        moveBase.style.left = moveStart.x + 'px';
        moveBase.style.top = moveStart.y + 'px';
        moveBase.style.display = 'block';
        moveThumb.style.left = moveStart.x + 'px';
        moveThumb.style.top = moveStart.y + 'px';
        moveThumb.style.display = 'block';
      }
    }
  }, { passive: false });

  window.addEventListener('touchmove', (e) => {
    for (let t of e.touches) {
      if (t.identifier === moveTouchId) {
        let dx = t.clientX - moveStart.x;
        let dy = t.clientY - moveStart.y;
        let dist = Math.hypot(dx, dy);
        if (dist > MAX_RADIUS) {
          dx = (dx / dist) * MAX_RADIUS;
          dy = (dy / dist) * MAX_RADIUS;
        }
        moveThumb.style.left = (moveStart.x + dx) + 'px';
        moveThumb.style.top = (moveStart.y + dy) + 'px';
        moveVector = { dx: dx / MAX_RADIUS, dy: dy / MAX_RADIUS };
      } else if (t.identifier === aimTouchId) {
        let dx = t.clientX - aimStart.x;
        let dy = t.clientY - aimStart.y;
        let dist = Math.hypot(dx, dy);
        if (dist > 10) {
          aimAngle = Math.atan2(dy, dx);
          isFiring = true;
        }
        if (dist > MAX_RADIUS) {
          dx = (dx / dist) * MAX_RADIUS;
          dy = (dy / dist) * MAX_RADIUS;
        }
        aimThumb.style.left = (aimStart.x + dx) + 'px';
        aimThumb.style.top = (aimStart.y + dy) + 'px';
      }
    }
  }, { passive: false });

  function releaseTouch(id) {
    if (id === moveTouchId) {
      moveTouchId = null;
      moveVector = { dx: 0, dy: 0 };
      moveBase.style.display = 'none';
      moveThumb.style.display = 'none';
    }
    if (id === aimTouchId) {
      aimTouchId = null;
      isFiring = false;
      aimBase.style.display = 'none';
      aimThumb.style.display = 'none';
    }
  }
  window.addEventListener('touchend', (e) => {
    for (let t of e.changedTouches) releaseTouch(t.identifier);
  });
  window.addEventListener('touchcancel', (e) => {
    for (let t of e.changedTouches) releaseTouch(t.identifier);
  });

  // Aim Touch
  aimZone.addEventListener('touchstart', (e) => {
    e.preventDefault();
    initAudio();
    for (let t of e.changedTouches) {
      if (aimTouchId === null) {
        aimTouchId = t.identifier;
        aimStart = { x: t.clientX, y: t.clientY };
        aimBase.style.left = aimStart.x + 'px';
        aimBase.style.top = aimStart.y + 'px';
        aimBase.style.display = 'block';
        aimThumb.style.left = aimStart.x + 'px';
        aimThumb.style.top = aimStart.y + 'px';
        aimThumb.style.display = 'block';
      }
    }
  }, { passive: false });

  function startJoin() {
    initAudio();
    const input = document.getElementById('name-input').value.trim();
    if (input) myName = input;
    document.getElementById('player-name-txt').textContent = myName;
    document.getElementById('join-overlay').style.display = 'none';
    connectSocket();
  }

  function connectSocket() {
    const loc = window.location;
    const wsUri = (loc.protocol === 'https:' ? 'wss:' : 'ws:') + '//' + loc.host + '/ws';
    ws = new WebSocket(wsUri);

    ws.onopen = () => {
      ws.send(JSON.stringify({ type: 'JOIN', name: myName, device: /iPhone|iPad|iPod/i.test(navigator.userAgent) ? 'iPhone' : 'Web' }));
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === 'WELCOME') {
          myId = msg.playerId;
        } else if (msg.type === 'STATE') {
          gameState = msg.state;
          updateHUD();
        } else if (msg.type === 'SFX') {
          if (msg.sfx === 'SHOOT') playTone(800, 260, 0.05);
          else if (msg.sfx === 'HIT') playTone(240, 90, 0.06);
          else if (msg.sfx === 'SHARD') playTone(580, 880, 0.1);
          else if (msg.sfx === 'CRYSTAL_HIT') playTone(900, 400, 0.2, 'sawtooth');
        }
      } catch(e) {}
    };

    ws.onclose = () => {
      setTimeout(connectSocket, 2000);
    };
  }

  // Periodic input packet send (30 times/sec)
  setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN && myId) {
      ws.send(JSON.stringify({
        type: 'INPUT',
        playerId: myId,
        dx: moveVector.dx,
        dy: moveVector.dy,
        aimAngle: aimAngle,
        isFiring: isFiring
      }));
    }
  }, 33);

  function sendUpgrade(type) {
    if (ws && ws.readyState === WebSocket.OPEN && myId) {
      ws.send(JSON.stringify({
        type: 'UPGRADE',
        playerId: myId,
        upgrade: type
      }));
    }
  }

  function updateHUD() {
    if (!gameState) return;
    const crystal = gameState.crystal;
    if (crystal) {
      const pct = Math.max(0, Math.min(100, (crystal.health / crystal.maxHealth) * 100));
      document.getElementById('crystal-bar').style.width = pct + '%';
    }
    if (gameState.wave) {
      document.getElementById('wave-txt').textContent = gameState.wave.waveNumber;
    }
    if (myId && gameState.players) {
      const me = gameState.players.find(p => p.id === myId);
      if (me) {
        document.getElementById('shard-txt').textContent = me.shards;
        document.getElementById('btn-up-repair').disabled = me.shards < 8;
        document.getElementById('btn-up-dmg').disabled = me.shards < 5;
        document.getElementById('btn-up-blast').disabled = me.shards < 10;
      }
    }
  }

  // Render Loop
  const PLAYER_COLORS = ['#00e5ff', '#ff4081', '#00e676', '#ffd700'];
  const ZOMBIE_COLORS = {
    WALKER: '#4caf50',
    RUNNER: '#ff5722',
    TANK: '#795548',
    SPITTER: '#cddc39',
    BOSS: '#9c27b0'
  };

  function render() {
    requestAnimationFrame(render);
    ctx.fillStyle = '#0a0e17';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    if (!gameState) {
      ctx.fillStyle = '#00e5ff';
      ctx.font = 'bold 18px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('Connecting to Cristal Zombie Host...', canvas.width / 2, canvas.height / 2);
      return;
    }

    // Determine camera focus: follow current player if available, else center on crystal
    let camX = 600, camY = 600;
    if (myId && gameState.players) {
      const me = gameState.players.find(p => p.id === myId);
      if (me) { camX = me.x; camY = me.y; }
    }

    const scale = Math.min(canvas.width, canvas.height) / 540;
    ctx.save();
    ctx.translate(canvas.width / 2, canvas.height / 2);
    ctx.scale(scale, scale);
    ctx.translate(-camX, -camY);

    // Draw Arena Grid
    ctx.strokeStyle = 'rgba(42, 59, 92, 0.4)';
    ctx.lineWidth = 1;
    const GRID_SIZE = 80;
    for (let x = 0; x <= 1200; x += GRID_SIZE) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, 1200); ctx.stroke();
    }
    for (let y = 0; y <= 1200; y += GRID_SIZE) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(1200, y); ctx.stroke();
    }
    // Arena Border
    ctx.strokeStyle = '#00e5ff';
    ctx.lineWidth = 4;
    ctx.strokeRect(0, 0, 1200, 1200);

    // Draw Central Crystal Core
    const crystal = gameState.crystal;
    if (crystal) {
      // Glow circle
      const grad = ctx.createRadialGradient(crystal.x, crystal.y, 10, crystal.x, crystal.y, crystal.radius * 2.2);
      grad.addColorStop(0, 'rgba(0, 229, 255, 0.6)');
      grad.addColorStop(0.5, 'rgba(179, 136, 255, 0.3)');
      grad.addColorStop(1, 'rgba(0, 229, 255, 0)');
      ctx.fillStyle = grad;
      ctx.beginPath(); ctx.arc(crystal.x, crystal.y, crystal.radius * 2.2, 0, Math.PI * 2); ctx.fill();

      // Crystal polygon
      ctx.fillStyle = '#00e5ff';
      ctx.beginPath();
      ctx.moveTo(crystal.x, crystal.y - crystal.radius);
      ctx.lineTo(crystal.x + crystal.radius, crystal.y);
      ctx.lineTo(crystal.x, crystal.y + crystal.radius);
      ctx.lineTo(crystal.x - crystal.radius, crystal.y);
      ctx.closePath();
      ctx.fill();

      // Crystal highlight
      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      ctx.moveTo(crystal.x, crystal.y - crystal.radius);
      ctx.lineTo(crystal.x + crystal.radius * 0.4, crystal.y);
      ctx.lineTo(crystal.x, crystal.y + crystal.radius * 0.3);
      ctx.closePath();
      ctx.fill();
    }

    // Draw Shards
    if (gameState.shards) {
      for (let s of gameState.shards) {
        ctx.fillStyle = '#ffd700';
        ctx.beginPath();
        ctx.arc(s.x, s.y, 6, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1;
        ctx.stroke();
      }
    }

    // Draw Projectiles
    if (gameState.projectiles) {
      for (let p of gameState.projectiles) {
        ctx.fillStyle = p.isEnemy ? '#ff5252' : '#00e5ff';
        ctx.shadowColor = p.isEnemy ? '#ff5252' : '#00e5ff';
        ctx.shadowBlur = 8;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius || 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;
      }
    }

    // Draw Zombies
    if (gameState.zombies) {
      for (let z of gameState.zombies) {
        const color = ZOMBIE_COLORS[z.type] || '#4caf50';
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.arc(z.x, z.y, z.radius, 0, Math.PI * 2);
        ctx.fill();

        // Eyes
        ctx.fillStyle = '#ff1744';
        ctx.beginPath();
        ctx.arc(z.x - z.radius * 0.3, z.y - z.radius * 0.2, 2.5, 0, Math.PI * 2);
        ctx.arc(z.x + z.radius * 0.3, z.y - z.radius * 0.2, 2.5, 0, Math.PI * 2);
        ctx.fill();

        // Health bar
        if (z.health < z.maxHealth) {
          const bw = z.radius * 2;
          ctx.fillStyle = 'rgba(0,0,0,0.6)';
          ctx.fillRect(z.x - z.radius, z.y - z.radius - 8, bw, 4);
          ctx.fillStyle = '#00e676';
          ctx.fillRect(z.x - z.radius, z.y - z.radius - 8, bw * (z.health / z.maxHealth), 4);
        }
      }
    }

    // Draw Players
    if (gameState.players) {
      for (let p of gameState.players) {
        const pColor = PLAYER_COLORS[p.colorIndex % PLAYER_COLORS.length];
        
        // Player direction barrel
        ctx.strokeStyle = pColor;
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.moveTo(p.x, p.y);
        ctx.lineTo(p.x + Math.cos(p.angle) * 24, p.y + Math.sin(p.angle) * 24);
        ctx.stroke();

        // Player body
        ctx.fillStyle = pColor;
        ctx.beginPath();
        ctx.arc(p.x, p.y, 16, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.stroke();

        // Name Tag
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 11px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(p.name + (p.id === myId ? " (YOU)" : ""), p.x, p.y - 22);

        // Player HP bar
        const hpWidth = 28;
        ctx.fillStyle = 'rgba(0,0,0,0.5)';
        ctx.fillRect(p.x - hpWidth/2, p.y - 20, hpWidth, 3);
        ctx.fillStyle = p.health > 30 ? '#00e676' : '#ff5252';
        ctx.fillRect(p.x - hpWidth/2, p.y - 20, hpWidth * (p.health / 100), 3);
      }
    }

    ctx.restore();
  }
  render();
</script>
</body>
</html>
        """.trimIndent()
    }
}
