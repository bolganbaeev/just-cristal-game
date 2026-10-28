package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_records")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val waveReached: Int,
    val zombiesKilled: Int,
    val crystalHealthLeft: Int,
    val survivalSeconds: Long,
    val playerCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
