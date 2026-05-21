package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AnimeSite
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeSiteDao {
    @Query("SELECT * FROM anime_sites ORDER BY trustScore DESC, name ASC")
    fun getAllSites(): Flow<List<AnimeSite>>

    @Query("SELECT * FROM anime_sites WHERE id = :id LIMIT 1")
    suspend fun getSiteById(id: Int): AnimeSite?

    @Query("SELECT * FROM anime_sites WHERE url = :url LIMIT 1")
    suspend fun getSiteByUrl(url: String): AnimeSite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: AnimeSite): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSites(sites: List<AnimeSite>)

    @Update
    suspend fun updateSite(site: AnimeSite)

    @Delete
    suspend fun deleteSite(site: AnimeSite)

    @Query("DELETE FROM anime_sites WHERE id = :id")
    suspend fun deleteSiteById(id: Int)

    @Query("SELECT COUNT(*) FROM anime_sites")
    suspend fun getCount(): Int
}
