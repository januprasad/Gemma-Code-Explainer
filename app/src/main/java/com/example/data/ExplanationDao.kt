package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExplanationDao {
    @Query("SELECT * FROM explanations ORDER BY timestamp DESC")
    fun getAllExplanations(): Flow<List<ExplanationEntity>>

    @Query("SELECT * FROM explanations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteExplanations(): Flow<List<ExplanationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExplanation(explanation: ExplanationEntity): Long

    @Query("UPDATE explanations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Delete
    suspend fun deleteExplanation(explanation: ExplanationEntity)

    @Query("DELETE FROM explanations")
    suspend fun deleteAllExplanations()
}
