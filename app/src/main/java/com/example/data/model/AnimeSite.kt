package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_sites")
data class AnimeSite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val type: String, // "legal" or "unofficial"
    val status: String, // "online", "down", "unknown"
    val trustScore: Int, // 0 to 100
    val category: String, // "Safe & Legal", "Community Mirror", "Suspicious Clone", "Malicious"
    val sslValid: Boolean = true,
    val virusTotalFlags: Int = 0,
    val lastPingTime: Long = System.currentTimeMillis(),
    val notes: String = "",
    val avgPingMs: Int = -1,
    val isFavorite: Boolean = false
)
