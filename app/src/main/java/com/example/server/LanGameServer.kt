package com.example.server

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class LanGameServer(
    private val context: Context,
    private val port: Int = 8080,
    private val onClientJoined: (clientId: String, name: String, device: String) -> Unit,
    private val onClientInput: (clientId: String, dx: Float, dy: Float, aimAngle: Float, isFiring: Boolean) -> Unit,
    private val onClientUpgrade: (clientId: String, upgradeType: String) -> Unit,
    private val onClientLeft: (clientId: String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val clients = CopyOnWriteArrayList<ConnectedClient>()

    data class ConnectedClient(
        val id: String,
        val socket: Socket,
        val output: OutputStream,
        var name: String = "Companion",
        var device: String = "Web"
    )

    fun start() {
        if (serverJob != null) return
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                Log.d("LanServer", "Server started on port $port")
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch { handleConnection(socket) }
                }
            } catch (e: Exception) {
                Log.e("LanServer", "Server exception: ${e.message}")
            }
        }
    }

    fun stop() {
        try {
            serverJob?.cancel()
            serverJob = null
            for (c in clients) {
                try { c.socket.close() } catch (_: Exception) {}
            }
            clients.clear()
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}
    }

    fun getConnectedClientCount(): Int = clients.size

    fun getConnectedClients(): List<ConnectedClient> = clients.toList()

    fun getLocalIpAddress(): String {
        try {
            // Check Wi-Fi manager first
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiIp = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (wifiIp != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    wifiIp and 0xff,
                    wifiIp shr 8 and 0xff,
                    wifiIp shr 16 and 0xff,
                    wifiIp shr 24 and 0xff
                )
            }
            // Fallback: iterate network interfaces
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun handleConnection(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()

        // Read HTTP request header
        val headerLines = mutableListOf<String>()
        var line = readLine(input)
        while (!line.isNullOrEmpty()) {
            headerLines.add(line)
            line = readLine(input)
        }

        if (headerLines.isEmpty()) {
            socket.close()
            return
        }

        val requestLine = headerLines[0]
        val parts = requestLine.split(" ")
        if (parts.size < 2) {
            socket.close()
            return
        }

        val method = parts[0]
        val path = parts[1]

        if (path.startsWith("/ws")) {
            // WebSocket Upgrade Handshake
            var secKey: String? = null
            for (h in headerLines) {
                if (h.startsWith("Sec-WebSocket-Key:", ignoreCase = true)) {
                    secKey = h.substringAfter(":").trim()
                    break
                }
            }

            if (secKey == null) {
                val badReq = "HTTP/1.1 400 Bad Request\r\n\r\n"
                output.write(badReq.toByteArray())
                output.flush()
                socket.close()
                return
            }

            // Compute Accept Key
            val guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
            val digest = MessageDigest.getInstance("SHA-1").digest((secKey + guid).toByteArray())
            val acceptKey = Base64.encodeToString(digest, Base64.NO_WRAP)

            val handshakeResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"

            output.write(handshakeResponse.toByteArray())
            output.flush()

            val clientId = "client_${System.currentTimeMillis() % 10000}_${(10..99).random()}"
            val client = ConnectedClient(id = clientId, socket = socket, output = output)
            clients.add(client)

            // Send welcome message
            val welcomeJson = JSONObject().apply {
                put("type", "WELCOME")
                put("playerId", clientId)
            }
            sendWsMessage(client, welcomeJson.toString())

            // Loop reading WebSocket frames
            try {
                handleWebSocketLoop(client, input)
            } catch (_: Exception) {
            } finally {
                clients.remove(client)
                onClientLeft(clientId)
                try { socket.close() } catch (_: Exception) {}
            }
        } else {
            // Serve Web Companion HTML
            val html = WebClientHtml.getHtml()
            val responseBytes = html.toByteArray(Charsets.UTF_8)
            val httpHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${responseBytes.size}\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(httpHeader.toByteArray())
            output.write(responseBytes)
            output.flush()
            socket.close()
        }
    }

    private fun handleWebSocketLoop(client: ConnectedClient, input: InputStream) {
        while (client.socket.isConnected && !client.socket.isClosed) {
            val b1 = input.read()
            if (b1 == -1) break
            val b2 = input.read()
            if (b2 == -1) break

            val opcode = b1 and 0x0F
            val isMasked = (b2 and 0x80) != 0
            var payloadLen = (b2 and 0x7F).toLong()

            if (payloadLen == 126L) {
                val byte1 = input.read()
                val byte2 = input.read()
                if (byte1 == -1 || byte2 == -1) break
                payloadLen = ((byte1 and 0xFF) shl 8 or (byte2 and 0xFF)).toLong()
            } else if (payloadLen == 127L) {
                // 8 bytes
                var len = 0L
                for (i in 0 until 8) {
                    val b = input.read()
                    if (b == -1) return
                    len = (len shl 8) or (b and 0xFF).toLong()
                }
                payloadLen = len
            }

            val mask = ByteArray(4)
            if (isMasked) {
                var bytesRead = 0
                while (bytesRead < 4) {
                    val r = input.read(mask, bytesRead, 4 - bytesRead)
                    if (r == -1) return
                    bytesRead += r
                }
            }

            val payload = ByteArray(payloadLen.toInt().coerceAtLeast(0))
            var totalRead = 0
            while (totalRead < payload.size) {
                val r = input.read(payload, totalRead, payload.size - totalRead)
                if (r == -1) break
                totalRead += r
            }

            if (isMasked) {
                for (i in payload.indices) {
                    payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
                }
            }

            when (opcode) {
                0x01 -> { // Text frame
                    val text = String(payload, Charsets.UTF_8)
                    handleClientMessage(client, text)
                }
                0x08 -> { // Close frame
                    break
                }
                0x09 -> { // Ping -> send Pong
                    sendWsPong(client, payload)
                }
            }
        }
    }

    private fun handleClientMessage(client: ConnectedClient, jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            when (json.optString("type")) {
                "JOIN" -> {
                    val name = json.optString("name", "Companion").take(12)
                    val device = json.optString("device", "Web")
                    client.name = name
                    client.device = device
                    onClientJoined(client.id, name, device)
                }
                "INPUT" -> {
                    val dx = json.optDouble("dx", 0.0).toFloat()
                    val dy = json.optDouble("dy", 0.0).toFloat()
                    val aimAngle = json.optDouble("aimAngle", 0.0).toFloat()
                    val isFiring = json.optBoolean("isFiring", false)
                    onClientInput(client.id, dx, dy, aimAngle, isFiring)
                }
                "UPGRADE" -> {
                    val upgradeType = json.optString("upgrade")
                    onClientUpgrade(client.id, upgradeType)
                }
            }
        } catch (e: Exception) {
            Log.e("LanServer", "Error parsing msg: ${e.message}")
        }
    }

    fun broadcastState(stateJson: String) {
        for (client in clients) {
            sendWsMessage(client, stateJson)
        }
    }

    fun broadcastSfx(sfxType: String) {
        val sfxJson = JSONObject().apply {
            put("type", "SFX")
            put("sfx", sfxType)
        }.toString()
        for (client in clients) {
            sendWsMessage(client, sfxJson)
        }
    }

    @Synchronized
    private fun sendWsMessage(client: ConnectedClient, message: String) {
        try {
            val bytes = message.toByteArray(Charsets.UTF_8)
            val out = client.output
            out.write(0x81) // FIN + text opcode

            val len = bytes.size
            if (len <= 125) {
                out.write(len)
            } else if (len <= 65535) {
                out.write(126)
                out.write((len shr 8) and 0xFF)
                out.write(len and 0xFF)
            } else {
                out.write(127)
                for (i in 7 downTo 0) {
                    out.write((len.toLong() shr (i * 8)).toInt() and 0xFF)
                }
            }
            out.write(bytes)
            out.flush()
        } catch (_: Exception) {}
    }

    private fun sendWsPong(client: ConnectedClient, payload: ByteArray) {
        try {
            val out = client.output
            out.write(0x8A) // Pong opcode
            out.write(payload.size)
            out.write(payload)
            out.flush()
        } catch (_: Exception) {}
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        var c = input.read()
        while (c != -1) {
            if (c == '\n'.code) {
                return sb.toString().trimEnd('\r')
            }
            sb.append(c.toChar())
            c = input.read()
        }
        return if (sb.isNotEmpty()) sb.toString() else null
    }
}
