package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.GameSimulation
import com.example.model.UpgradeType
import com.example.model.WaveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cristal Zombie", appName)
    }

    @Test
    fun `simulation initialization and player addition`() {
        val sim = GameSimulation(
            onSfxTriggered = {},
            onGameOver = { _, _, _ -> }
        )
        val player = sim.addPlayer(
            id = "test_player",
            name = "Test Defender",
            deviceType = "iPhone",
            isHost = false
        )
        assertNotNull(player)
        assertEquals(1, sim.players.size)
        assertEquals(1000f, sim.crystal.health)
        assertEquals(WaveStatus.COUNTDOWN, sim.wave.status)
    }

    @Test
    fun `upgrade mechanics and shard deduction`() {
        val sim = GameSimulation(
            onSfxTriggered = {},
            onGameOver = { _, _, _ -> }
        )
        val player = sim.addPlayer(
            id = "player_1",
            name = "Hero",
            deviceType = "Android",
            isHost = true
        )
        player.shards = 20

        val success = sim.applyUpgrade("player_1", UpgradeType.WEAPON_DAMAGE.name)
        assertTrue(success)
        assertEquals(2, player.weaponDamageLevel)
        assertEquals(15, player.shards) // 20 - 5 = 15
    }

    @Test
    fun `state json serialization`() {
        val sim = GameSimulation(
            onSfxTriggered = {},
            onGameOver = { _, _, _ -> }
        )
        sim.addPlayer("p1", "Commander", "Android", true)
        val json = sim.serializeStateJson()
        assertTrue(json.contains("\"type\":\"STATE\""))
        assertTrue(json.contains("\"crystal\""))
        assertTrue(json.contains("\"players\""))
    }
}
