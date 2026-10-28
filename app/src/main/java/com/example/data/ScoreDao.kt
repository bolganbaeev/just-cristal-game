package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertRecord(record: ScoreRecord): Long

    @Query("SELECT * FROM score_records ORDER BY waveReached DESC, survivalSeconds DESC LIMIT 10")
    fun getTopScores(): Flow<List<ScoreRecord>>

    @Query("SELECT MAX(waveReached) FROM score_records")
    fun getHighestWave(): Flow<Int?>

    @Query("SELECT SUM(zombiesKilled) FROM score_records")
    fun getTotalZombiesKilled(): Flow<Int?>
}
