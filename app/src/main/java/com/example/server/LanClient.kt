package com.example.server

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LanClient(
    private val onStateReceived: (jsonString: String) -> Unit,
    private val onConnected: (assignedPlayerId: String) -> Unit,
    private val onDisconnected: () -> Unit,
    private val onSfxReceived: (sfxType: String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var webSocket: WebSocket? = null
    var myPlayerId: String? = null

    fun connect(hostIp: String, port: Int = 8080, playerName: String) {
        val request = Request.Builder()
            .url("ws://$hostIp:$port/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("LanClient", "Connected to host $hostIp:$port")
                val joinMsg = JSONObject().apply {
                    put("type", "JOIN")
                    put("name", playerName)
                    put("device", "Android")
                }
                ws.send(joinMsg.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "WELCOME" -> {
                            val id = json.optString("playerId")
                            myPlayerId = id
                            onConnected(id)
                        }
                        "STATE" -> {
                            onStateReceived(text)
                        }
                        "SFX" -> {
                            onSfxReceived(json.optString("sfx"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LanClient", "Error handling message: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("LanClient", "WebSocket failure: ${t.message}")
                onDisconnected()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                onDisconnected()
            }
        })
    }

    fun sendInput(dx: Float, dy: Float, aimAngle: Float, isFiring: Boolean) {
        val id = myPlayerId ?: return
        val msg = JSONObject().apply {
            put("type", "INPUT")
            put("playerId", id)
            put("dx", dx.toDouble())
            put("dy", dy.toDouble())
            put("aimAngle", aimAngle.toDouble())
            put("isFiring", isFiring)
        }
        webSocket?.send(msg.toString())
    }

    fun sendUpgrade(upgradeType: String) {
        val id = myPlayerId ?: return
        val msg = JSONObject().apply {
            put("type", "UPGRADE")
            put("playerId", id)
            put("upgrade", upgradeType)
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Leaving")
            webSocket = null
        } catch (_: Exception) {}
    }
}
