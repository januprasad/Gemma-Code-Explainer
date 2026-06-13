package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "explanations")
data class ExplanationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val code: String,
    val explanation: String,
    val language: String,
    val actionType: String,
    val timestamp: Long,
    val isFavorite: Boolean = false
)
